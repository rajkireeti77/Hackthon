import argparse
import json
import re
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn


SYSTEM_PROMPT = (
    "You are PhishGuard AI, a cybersecurity assistant. "
    "Return only strict JSON with keys verdict, riskScore, severity, confidence, summary, redFlags, recommendedActions."
)


class AnalyzeRequest(BaseModel):
    textContent: str = ""
    url: str = ""
    fileContext: str = ""
    safeBrowsingFlagged: bool = False
    threatTypes: list[str] = []


def build_prompt(req: AnalyzeRequest) -> str:
    return (
        f"{SYSTEM_PROMPT}\n"
        f"Analyze this input.\n"
        f"textContent={req.textContent}\n"
        f"url={req.url}\n"
        f"fileContext={req.fileContext}\n"
        f"safeBrowsingFlagged={str(req.safeBrowsingFlagged).lower()}\n"
        f"threatTypes={req.threatTypes}\n"
    )


def extract_json(text: str) -> dict[str, Any]:
    match = re.search(r"\{.*\}", text, flags=re.DOTALL)
    if not match:
        raise ValueError("No JSON object found in model output.")
    data = json.loads(match.group(0))
    data.setdefault("redFlags", [])
    data.setdefault("recommendedActions", [])
    data.setdefault("summary", "")
    data.setdefault("confidence", 0.5)
    data.setdefault("riskScore", 50)
    data.setdefault("severity", "Medium")
    data.setdefault("verdict", "Suspicious")
    return data


def create_app(base_model: str, adapter_dir: str | None):
    try:
        from peft import PeftModel
        from transformers import AutoModelForCausalLM, AutoTokenizer, pipeline
    except ImportError as exc:
        raise SystemExit("Missing inference dependencies. Install backend/ml/requirements-llm.txt first.") from exc

    tokenizer = AutoTokenizer.from_pretrained(adapter_dir or base_model)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    model = AutoModelForCausalLM.from_pretrained(base_model)
    if adapter_dir:
        model = PeftModel.from_pretrained(model, adapter_dir)

    generator = pipeline(
        "text-generation",
        model=model,
        tokenizer=tokenizer,
        max_new_tokens=256,
        do_sample=False,
    )

    app = FastAPI(title="PhishGuard Local LLM")

    @app.get("/health")
    def health():
        return {"ok": True, "baseModel": base_model, "adapterDir": adapter_dir}

    @app.post("/analyze")
    def analyze(req: AnalyzeRequest):
        prompt = build_prompt(req)
        output = generator(prompt)[0]["generated_text"]
        payload = extract_json(output[len(prompt):] if output.startswith(prompt) else output)
        return payload

    return app


def main():
    from pathlib import Path

    base_dir = Path(__file__).resolve().parent
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-model", default="TinyLlama/TinyLlama-1.1B-Chat-v1.0")
    ap.add_argument("--adapter-dir", default=str(base_dir / "llm-artifacts" / "phishguard-lora"))
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("--port", type=int, default=8001)
    args = ap.parse_args()

    app = create_app(args.base_model, args.adapter_dir)
    uvicorn.run(app, host=args.host, port=args.port)


if __name__ == "__main__":
    main()
