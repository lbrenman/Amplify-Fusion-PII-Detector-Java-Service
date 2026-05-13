# Amplify Fusion — PII Detector Java Service

A reusable **Amplify Fusion Java service** that detects and redacts Personally Identifiable Information (PII) from text using two complementary approaches:

- **String scanning** — detects structured PII (email, SSN, credit card, phone, IP, ZIP) using pure character-level parsing
- **OpenNLP NER** — detects unstructured PII (person names, organizations, locations) in free-form prose

Requires a **Dedicated Data Plane** with OpenNLP JARs and model files uploaded.

A shared data plane version with string scanning only and no JARs required is available [here](https://gist.github.com/lbrenman/6a78f1b51657e6f5b93bd859ae3593ac).

A sample project export for both are available in this repo. Import the zip file(s) into your Fusion Tenant to test.

---

## Detected PII Types

### Structured (string scanning)

| Type | Examples |
|------|---------|
| `EMAIL` | `john.doe@example.com` |
| `SSN` | `123-45-6789`, `123456789` |
| `CREDIT_CARD` | Visa, Mastercard, Amex, Discover (13–16 digits) |
| `PHONE` | `415-555-0101`, `(800) 555-0199` |
| `IP_ADDRESS` | `192.168.1.1`, `203.0.113.47` |
| `ZIP_CODE` | `94107`, `10001-1234` |

### Unstructured (OpenNLP NER)

| Type | Examples |
|------|---------|
| `PERSON` | `John Smith`, `Dr. Sarah Connor` |
| `ORGANIZATION` | `Acme Corporation`, `IBM`, `Microsoft` |
| `LOCATION` | `San Francisco`, `New York`, `Seattle` |

---

## Required JARs

Upload all files via: **Manager → Environments → [Data Plane] → Libraries → Upload a Jar**

Upload in this order:

| # | File | Download |
|---|------|---------|
| 1 | `slf4j-api-1.7.36.jar` | https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar |
| 2 | `slf4j-simple-1.7.36.jar` | https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar |
| 3 | `opennlp-tools-2.3.3.jar` | https://repo1.maven.org/maven2/org/apache/opennlp/opennlp-tools/2.3.3/opennlp-tools-2.3.3.jar |
| 4 | `opennlp-ner-models.jar` | *(build locally — see below)* |

### Building the Model JAR

The NER model `.bin` files are not available as pre-built JARs on Maven Central. Download them from SourceForge and package them manually:

Download the three model files directly from SourceForge:

| Model | Download |
|-------|---------|
| `en-ner-person.bin` | https://opennlp.sourceforge.net/en-ner-person.bin |
| `en-ner-organization.bin` | https://opennlp.sourceforge.net/en-ner-organization.bin |
| `en-ner-location.bin` | https://opennlp.sourceforge.net/en-ner-location.bin |

> **Note:** The browser may block the `.bin` downloads as unrecognized file types. Click **Keep** when prompted to allow the download.

Then package them into a JAR:

```bash
# Package into a JAR
mkdir -p models
cp en-ner-person.bin en-ner-organization.bin en-ner-location.bin models/
jar cf opennlp-ner-models.jar -C . models/

# Verify contents
jar tf opennlp-ner-models.jar
```

Expected output:
```
META-INF/
META-INF/MANIFEST.MF
models/
models/en-ner-location.bin
models/en-ner-organization.bin
models/en-ner-person.bin
```

The service loads models via `getClass().getClassLoader().getResourceAsStream("models/en-ner-{type}.bin")` — the internal JAR paths above must match exactly.

---

## Inputs & Outputs

### Input Variables

| Name | Type | Description |
|------|------|-------------|
| `textInput` | String | The text to scan for PII |
| `freeTextFields` | String | Comma-separated field names containing the text (for audit trail) |

`freeTextFields`is purely an **audit trail** input — it doesn't affect detection or redaction at all.

You pass in the names of the fields in your pipeline that contain the text being scanned (e.g. `"notes, comments, description"`), and the service echoes them back in `detectionReport.freeTextFields`. This way, downstream systems or audit logs can see exactly which source fields were scanned to produce the redacted output.

For example, if your pipeline has three text fields and you concatenate them all into `textInput` before calling the service, `freeTextFields` lets you record that the scan covered `notes`, `comments`, and `description` — rather than just knowing that *some* text was scanned.

It has no effect on what gets detected or redacted. If you don't need that audit trail, you can leave it empty or remove it entirely — the service works the same either way.

### Output Variables

| Name | Type | Description |
|------|------|-------------|
| `piiDetected` | Boolean | `true` if any PII was found |
| `nerDetected` | Boolean | `true` if OpenNLP detected names, orgs, or locations |
| `redactedText` | String | Input text with all PII replaced by labeled placeholders |
| `detectionReport` | Document | Structured report — map fields directly in pipeline |

### `detectionReport` Document Fields

| Field | Type | Description |
|-------|------|-------------|
| `piiDetected` | Boolean | Overall detection flag |
| `nerDetected` | Boolean | NER-specific flag |
| `detectedTypes` | Array | List of detected PII type names |
| `counts` | Document | Per-type match counts e.g. `{ "EMAIL": 2, "PERSON": 1 }` |
| `freeTextFields` | Array | Field names passed in via input |
| `nerError` | String | Model load errors if any, empty string if clean |

### `detectionReport` Sample

```json
{
  "detectionReport": {
    "piiDetected": true,
    "nerDetected": true,
    "detectedTypes": [
      "EMAIL",
      "SSN",
      "CREDIT_CARD",
      "PHONE",
      "PERSON",
      "ORGANIZATION",
      "LOCATION"
    ],
    "counts": {
      "EMAIL": 1,
      "SSN": 1,
      "CREDIT_CARD": 1,
      "PHONE": 1,
      "PERSON": 1,
      "ORGANIZATION": 1,
      "LOCATION": 1
    },
    "freeTextFields": [
      "notes",
      "comments"
    ],
    "nerError": ""
  }
}
```

---

## Setup in Amplify Fusion

1. Upload all four JARs to your dedicated data plane (see above)
2. Navigate to **Services → Java Service → New**
3. Set the class name: `{ProjectName}_{ServiceName}` (e.g. `MyProject_PIIDetectorFull`)
4. Add input and output variables as listed above — set `detectionReport` type to **Document**
5. Paste the contents of [`PIIDetectorFull.java`](./PIIDetectorFull.java) into the code editor
6. Click **Validate**, then **Save**

---

## Test Examples

### Full mixed PII — structured + NER
```json
{
  "textInput": "Contact John Smith at john.smith@example.com or 415-555-0192. His SSN is 123-45-6789. He works at Acme Corporation in San Francisco. Card: 4111111111111111.",
  "freeTextFields": "notes"
}
```
**Redacted:**
```
Contact [NAME REDACTED] at [EMAIL REDACTED] or [PHONE REDACTED]. His SSN is [SSN REDACTED]. He works at [ORG REDACTED] in [LOCATION REDACTED]. Card: [CREDIT CARD REDACTED].
```

---

### NER only — no structured PII
```json
{
  "textInput": "Dr. Sarah Connor met with representatives from IBM at the New York office to discuss the merger.",
  "freeTextFields": "meetingNotes"
}
```
**Redacted:**
```
Dr. [NAME REDACTED] met with representatives from [ORG REDACTED] at the [LOCATION REDACTED] office to discuss the merger.
```

---

### Structured PII only — no names
```json
{
  "textInput": "Card 5105105105105100 charged at ZIP 10001. Server: 192.168.0.55. Contact: billing@acme.com.",
  "freeTextFields": ""
}
```
**Redacted:**
```
Card [CREDIT CARD REDACTED] charged at ZIP [ZIP REDACTED]. Server: [IP REDACTED]. Contact: [EMAIL REDACTED].
```

---

### Multiple people and orgs
```json
{
  "textInput": "Alice Johnson and Bob Martinez from Google met with representatives of Microsoft in Seattle.",
  "freeTextFields": "transcript"
}
```
**Redacted:**
```
[NAME REDACTED] and [NAME REDACTED] from [ORG REDACTED] met with representatives of [ORG REDACTED] in [LOCATION REDACTED].
```

---

### Clean text — no PII
```json
{
  "textInput": "The quarterly review showed strong performance across all product lines.",
  "freeTextFields": ""
}
```
**Result:** Text unchanged, `piiDetected: false`, `nerDetected: false`.

---

## Implementation Notes

### Execution Order

Structured PII is redacted first via string scanning, then OpenNLP NER runs on the already-cleaned text. This prevents NER from tokenizing around partially-redacted values and reduces false positives.

### NER Re-tokenization

After each entity type pass (PERSON → ORGANIZATION → LOCATION), the redacted text is re-tokenized so each subsequent pass operates on the updated output.

### Model Loading

Models are loaded from the classpath using `getClass().getClassLoader().getResourceAsStream()`. This is how Fusion exposes uploaded JAR contents at runtime. The path `models/en-ner-person.bin` must match the internal structure of `opennlp-ner-models.jar` exactly.

### `nerError` Field

If a model file is missing or fails to load, the error is captured in `detectionReport.nerError` rather than crashing the service. Structured PII detection still completes even if NER models are unavailable.

### `detectionReport` as Document

The report is returned as a `Map<String, Object>` which Fusion surfaces as a **Document** type output. Individual fields (`piiDetected`, `counts`, `detectedTypes`, etc.) can be mapped directly to pipeline variables without parsing a JSON string.

---

## Known Limitations

| Limitation | Reason |
|------------|--------|
| NER accuracy varies | OpenNLP models are trained on news corpus (MUC-7); accuracy lower on informal text, abbreviations, or domain-specific names |
| Phone detection covers US formats only | International formats vary too widely for string scanning |
| ZIP codes may over-match | Any standalone 5-digit number qualifies |
| NER may miss uncommon name spellings | Model confidence thresholds apply |
| PASSPORT / DRIVERS_LICENSE / IBAN omitted | High false-positive rate without regex lookahead support |

---

## Compatibility

| | |
|--|--|
| Amplify Fusion Data Plane | Dedicated only |
| External JARs required | Yes — 4 files (~16 MB total) |
| Java version | 8+ |
| OpenNLP version | 2.3.3 |
| NER model series | 1.5 (MUC-7 trained) |