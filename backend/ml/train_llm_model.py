import argparse
import json
from pathlib import Path


def load_jsonl(path: Path) -> list[dict]:
    rows = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                rows.append(json.loads(line))
    return rows


def render_messages(messages: list[dict]) -> str:
    parts = []
    for message in messages:
        role = message.get("role", "user").upper()
        content = message.get("content", "")
        parts.append(f"{role}: {content}")
    return "\n".join(parts)


def main():
    base_dir = Path(__file__).resolve().parent
    ap = argparse.ArgumentParser()
    ap.add_argument("--train-jsonl", default=str(base_dir / "llm-data" / "train.jsonl"))
    ap.add_argument("--valid-jsonl", default=str(base_dir / "llm-data" / "valid.jsonl"))
    ap.add_argument("--base-model", default="TinyLlama/TinyLlama-1.1B-Chat-v1.0")
    ap.add_argument("--out-dir", default=str(base_dir / "llm-artifacts" / "phishguard-lora"))
    ap.add_argument("--epochs", type=int, default=1)
    ap.add_argument("--batch-size", type=int, default=1)
    ap.add_argument("--grad-accum", type=int, default=8)
    ap.add_argument("--learning-rate", type=float, default=2e-4)
    ap.add_argument("--max-seq-length", type=int, default=1024)
    args = ap.parse_args()

    try:
        from datasets import Dataset
        from peft import LoraConfig
        from transformers import AutoModelForCausalLM, AutoTokenizer, TrainingArguments
        from trl import SFTTrainer
    except ImportError as exc:
        raise SystemExit(
            "Missing LLM training dependencies. Install backend/ml/requirements-llm.txt first."
        ) from exc

    train_rows = load_jsonl(Path(args.train_jsonl))
    valid_rows = load_jsonl(Path(args.valid_jsonl))
    if not train_rows:
        raise SystemExit("Training dataset is empty.")

    tokenizer = AutoTokenizer.from_pretrained(args.base_model)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    model = AutoModelForCausalLM.from_pretrained(args.base_model)

    train_dataset = Dataset.from_list([{"text": render_messages(row["messages"])} for row in train_rows])
    valid_dataset = Dataset.from_list([{"text": render_messages(row["messages"])} for row in valid_rows])

    peft_config = LoraConfig(
        r=16,
        lora_alpha=32,
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj", "gate_proj", "up_proj", "down_proj"],
    )

    training_args = TrainingArguments(
        output_dir=args.out_dir,
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        gradient_accumulation_steps=args.grad_accum,
        learning_rate=args.learning_rate,
        logging_steps=10,
        eval_strategy="epoch",
        save_strategy="epoch",
        report_to=[],
        fp16=False,
        bf16=False,
    )

    trainer = SFTTrainer(
        model=model,
        tokenizer=tokenizer,
        train_dataset=train_dataset,
        eval_dataset=valid_dataset,
        dataset_text_field="text",
        max_seq_length=args.max_seq_length,
        peft_config=peft_config,
        args=training_args,
    )

    trainer.train()
    trainer.model.save_pretrained(args.out_dir)
    tokenizer.save_pretrained(args.out_dir)
    print(f"Saved LoRA adapter -> {args.out_dir}")


if __name__ == "__main__":
    main()
