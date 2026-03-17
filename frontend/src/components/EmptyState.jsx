import React from "react";

export default function EmptyState({ title, description, action }) {
  return (
    <div className="empty">
      <div className="empty-title">{title}</div>
      {description ? <div className="muted">{description}</div> : null}
      {action ? <div className="empty-action">{action}</div> : null}
    </div>
  );
}

