import http from "node:http";
import { chromium } from "playwright";

const PORT = Number(process.env.PORT || 8002);
const HOST = process.env.HOST || "0.0.0.0";
const NAVIGATION_TIMEOUT_MS = Number(process.env.NAVIGATION_TIMEOUT_MS || 15000);

const KEYWORDS = ["login", "verify", "password", "otp", "bank", "secure", "confirm", "update", "signin", "reset"];

function json(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type"
  });
  res.end(JSON.stringify(body));
}

function parseBody(req) {
  return new Promise((resolve, reject) => {
    let data = "";
    req.on("data", (chunk) => {
      data += chunk;
      if (data.length > 1024 * 1024) {
        reject(new Error("Payload too large"));
      }
    });
    req.on("end", () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch (err) {
        reject(err);
      }
    });
    req.on("error", reject);
  });
}

function clamp(value, low = 0, high = 100) {
  return Math.max(low, Math.min(high, value));
}

function verdictFor(score) {
  if (score >= 60) return "Malicious";
  if (score >= 35) return "Suspicious";
  return "Safe";
}

function countSuspiciousKeywords(text) {
  const lower = String(text || "").toLowerCase();
  return KEYWORDS.filter((keyword) => lower.includes(keyword));
}

async function analyzeUrl(url) {
  const browser = await chromium.launch({
    headless: true,
    chromiumSandbox: true
  });

  let context;
  try {
    context = await browser.newContext({
      ignoreHTTPSErrors: false,
      javaScriptEnabled: true,
      viewport: { width: 1440, height: 900 }
    });
    const page = await context.newPage();
    page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);

    let downloadAttempted = false;
    const redirectChain = [];

    page.on("download", () => {
      downloadAttempted = true;
    });
    page.on("response", (response) => {
      if (response.request().isNavigationRequest()) {
        redirectChain.push(response.url());
      }
    });

    const response = await page.goto(url, { waitUntil: "domcontentloaded" });
    let networkIdleReached = true;
    try {
      await page.waitForLoadState("networkidle", { timeout: 5000 });
    } catch {
      networkIdleReached = false;
    }

    const finalUrl = page.url();
    const title = await page.title().catch(() => "");
    const bodyText = await page.locator("body").innerText().catch(() => "");
    const loginFormDetected = await page
      .locator("input[type='password'], input[name*='password' i], form")
      .count()
      .then((count) => count > 0)
      .catch(() => false);

    const suspiciousKeywords = countSuspiciousKeywords(`${title}\n${bodyText}\n${finalUrl}`);
    const maliciousIndicators = [];

    let riskScore = 10;
    if (!response) {
      riskScore += 10;
      maliciousIndicators.push("No initial response");
    }
    if (!finalUrl.startsWith("https://")) {
      riskScore += 10;
      maliciousIndicators.push("No HTTPS on final URL");
    }
    if (redirectChain.length >= 3) {
      riskScore += 12;
      maliciousIndicators.push("Multiple redirects");
    }
    if (loginFormDetected) {
      riskScore += 20;
      maliciousIndicators.push("Login form detected");
    }
    if (downloadAttempted) {
      riskScore += 25;
      maliciousIndicators.push("Download attempt");
    }
    if (suspiciousKeywords.length) {
      riskScore += Math.min(20, suspiciousKeywords.length * 4);
      maliciousIndicators.push("Suspicious page keywords");
    }
    if (finalUrl !== url) {
      maliciousIndicators.push("Redirected to a different URL");
    }

    riskScore = clamp(riskScore);
    const verdict = verdictFor(riskScore);
    const summary =
      verdict === "Malicious"
        ? "The URL behaved suspiciously in the isolated browser and should be considered unsafe."
        : verdict === "Suspicious"
          ? "The isolated browser observed risky behavior and the URL should be verified carefully."
          : "The isolated browser did not observe strong malicious behavior for this URL.";

    return {
      enabled: true,
      attempted: true,
      reachable: true,
      finalUrl,
      redirectCount: Math.max(0, redirectChain.length - 1),
      loginFormDetected,
      downloadAttempted,
      networkIdleReached,
      title,
      riskScore,
      verdict,
      summary,
      suspiciousKeywords,
      maliciousIndicators
    };
  } finally {
    if (context) {
      await context.close().catch(() => {});
    }
    await browser.close().catch(() => {});
  }
}

const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "Content-Type",
      "Access-Control-Allow-Methods": "POST,GET,OPTIONS"
    });
    res.end();
    return;
  }

  if (req.method === "GET" && req.url === "/health") {
    json(res, 200, { ok: true });
    return;
  }

  if (req.method === "POST" && req.url === "/analyze") {
    try {
      const body = await parseBody(req);
      const url = String(body?.url || "").trim();
      if (!/^https?:\/\//i.test(url)) {
        json(res, 400, { message: "URL must start with http:// or https://" });
        return;
      }
      const result = await analyzeUrl(url);
      json(res, 200, result);
    } catch (error) {
      json(res, 500, {
        enabled: true,
        attempted: true,
        reachable: false,
        riskScore: 0,
        verdict: "UNAVAILABLE",
        summary: error?.message || "Sandbox scanner failed.",
        suspiciousKeywords: [],
        maliciousIndicators: []
      });
    }
    return;
  }

  json(res, 404, { message: "Not found" });
});

server.listen(PORT, HOST, () => {
  console.log(`Sandbox scanner listening on http://${HOST}:${PORT}`);
});
