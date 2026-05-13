import java.util.*;
// Please do not change pre-populated code.
public class PIIDetectorFullJavaService_PIIDetectorFull {

    public Map<String,Object> execute(Map<String,Object> dataIn) {
        Map<String,Object> dataOut = new HashMap<String, Object>();

/* Please start your code here */

        // --- Inputs ---
        String text = dataIn.get("textInput") != null
            ? dataIn.get("textInput").toString() : "";

        String freeTextFieldsRaw = dataIn.get("freeTextFields") != null
            ? dataIn.get("freeTextFields").toString() : "";

        List<String> detectedTypes = new ArrayList<>();
        Map<String, Integer> detectedCounts = new LinkedHashMap<>();
        String redacted = text;

        // ============================================================
        // PART 1 - STRUCTURED PII (string scanning, no regex)
        // ============================================================

        // --- EMAIL ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                int atIdx = redacted.indexOf('@', i);
                if (atIdx < 0) { sb.append(redacted.substring(i)); break; }
                int start = atIdx - 1;
                while (start >= 0) {
                    char c = redacted.charAt(start);
                    if (Character.isLetterOrDigit(c) || c == '.' || c == '_'
                            || c == '%' || c == '+' || c == '-') { start--; } else { break; }
                }
                start++;
                int end = atIdx + 1;
                while (end < redacted.length()) {
                    char c = redacted.charAt(end);
                    if (Character.isLetterOrDigit(c) || c == '.' || c == '-') { end++; } else { break; }
                }
                String domain = redacted.substring(atIdx + 1, end);
                if (atIdx > start && domain.indexOf('.') >= 0 && domain.length() > 2) {
                    sb.append(redacted.substring(i, start));
                    sb.append("[EMAIL REDACTED]");
                    count++; i = end;
                } else {
                    sb.append(redacted.substring(i, atIdx + 1)); i = atIdx + 1;
                }
            }
            if (count > 0) {
                detectedTypes.add("EMAIL"); detectedCounts.put("EMAIL", count);
                redacted = sb.toString();
            }
        }

        // --- SSN: NNN-NN-NNNN or 9 plain digits ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                if (i + 9 > redacted.length()) { sb.append(redacted.substring(i)); break; }
                if (i > 0 && Character.isLetterOrDigit(redacted.charAt(i - 1))) {
                    sb.append(redacted.charAt(i)); i++; continue;
                }
                boolean found = false;
                if (i + 11 <= redacted.length()) {
                    String c = redacted.substring(i, i + 11);
                    if (isDigits(c.substring(0,3)) && c.charAt(3) == '-'
                            && isDigits(c.substring(4,6)) && c.charAt(6) == '-'
                            && isDigits(c.substring(7,11))) {
                        boolean endOk = i+11 >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(i+11));
                        if (endOk) { sb.append("[SSN REDACTED]"); count++; i += 11; found = true; }
                    }
                }
                if (!found && i + 9 <= redacted.length() && isDigits(redacted.substring(i, i+9))) {
                    boolean endOk = i+9 >= redacted.length()
                        || !Character.isLetterOrDigit(redacted.charAt(i+9));
                    if (endOk) { sb.append("[SSN REDACTED]"); count++; i += 9; found = true; }
                }
                if (!found) { sb.append(redacted.charAt(i)); i++; }
            }
            if (count > 0) {
                detectedTypes.add("SSN"); detectedCounts.put("SSN", count);
                redacted = sb.toString();
            }
        }

        // --- CREDIT CARD: 13-16 contiguous digits ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                if (!Character.isDigit(redacted.charAt(i))) { sb.append(redacted.charAt(i)); i++; continue; }
                int start = i;
                while (i < redacted.length() && Character.isDigit(redacted.charAt(i))) i++;
                int len = i - start;
                boolean startOk = start == 0 || !Character.isLetterOrDigit(redacted.charAt(start-1));
                boolean endOk = i >= redacted.length() || !Character.isLetterOrDigit(redacted.charAt(i));
                if (len >= 13 && len <= 16 && startOk && endOk) {
                    sb.append("[CREDIT CARD REDACTED]"); count++;
                } else { sb.append(redacted.substring(start, i)); }
            }
            if (count > 0) {
                detectedTypes.add("CREDIT_CARD"); detectedCounts.put("CREDIT_CARD", count);
                redacted = sb.toString();
            }
        }

        // --- PHONE: NNN-NNN-NNNN or (NNN) NNN-NNNN ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                boolean found = false;
                int start = i;
                if (i < redacted.length() && redacted.charAt(i) == '+') i++;
                if (i < redacted.length() && redacted.charAt(i) == '1'
                        && i+1 < redacted.length() && !Character.isDigit(redacted.charAt(i+1))) {
                    i++;
                    if (i < redacted.length() && (redacted.charAt(i) == ' '
                            || redacted.charAt(i) == '-')) i++;
                }
                if (i < redacted.length() && redacted.charAt(i) == '(' && i+14 <= redacted.length()) {
                    if (isDigits(redacted.substring(i+1,i+4)) && redacted.charAt(i+4) == ')'
                            && redacted.charAt(i+5) == ' ' && isDigits(redacted.substring(i+6,i+9))
                            && redacted.charAt(i+9) == '-' && isDigits(redacted.substring(i+10,i+14))) {
                        boolean endOk = i+14 >= redacted.length()
                            || !Character.isDigit(redacted.charAt(i+14));
                        if (endOk) {
                            sb.append(redacted.substring(start, i));
                            sb.append("[PHONE REDACTED]"); count++; i += 14; found = true;
                        }
                    }
                }
                if (!found && i+12 <= redacted.length()) {
                    if (isDigits(redacted.substring(i,i+3)) && redacted.charAt(i+3) == '-'
                            && isDigits(redacted.substring(i+4,i+7)) && redacted.charAt(i+7) == '-'
                            && isDigits(redacted.substring(i+8,i+12))) {
                        boolean startOk = i == 0 || !Character.isLetterOrDigit(redacted.charAt(i-1));
                        boolean endOk = i+12 >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(i+12));
                        if (startOk && endOk) {
                            sb.append(redacted.substring(start, i));
                            sb.append("[PHONE REDACTED]"); count++; i += 12; found = true;
                        }
                    }
                }
                if (!found) { i = start; sb.append(redacted.charAt(i)); i++; }
            }
            if (count > 0) {
                detectedTypes.add("PHONE"); detectedCounts.put("PHONE", count);
                redacted = sb.toString();
            }
        }

        // --- IP ADDRESS ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                boolean found = false;
                if (Character.isDigit(redacted.charAt(i))) {
                    int start = i;
                    int end1 = i;
                    while (end1 < redacted.length() && Character.isDigit(redacted.charAt(end1))) end1++;
                    if (end1 < redacted.length() && redacted.charAt(end1) == '.' && end1-i <= 3) {
                        int o1 = Integer.parseInt(redacted.substring(i, end1));
                        int j = end1+1; int end2 = j;
                        while (end2 < redacted.length() && Character.isDigit(redacted.charAt(end2))) end2++;
                        if (end2 < redacted.length() && redacted.charAt(end2) == '.'
                                && end2-j <= 3 && end2 > j) {
                            int o2 = Integer.parseInt(redacted.substring(j, end2));
                            int k = end2+1; int end3 = k;
                            while (end3 < redacted.length() && Character.isDigit(redacted.charAt(end3))) end3++;
                            if (end3 < redacted.length() && redacted.charAt(end3) == '.'
                                    && end3-k <= 3 && end3 > k) {
                                int o3 = Integer.parseInt(redacted.substring(k, end3));
                                int l = end3+1; int end4 = l;
                                while (end4 < redacted.length() && Character.isDigit(redacted.charAt(end4))) end4++;
                                if (end4 > l && end4-l <= 3) {
                                    int o4 = Integer.parseInt(redacted.substring(l, end4));
                                    boolean valid = o1<=255 && o2<=255 && o3<=255 && o4<=255;
                                    boolean endOk = end4 >= redacted.length()
                                        || !Character.isLetterOrDigit(redacted.charAt(end4));
                                    boolean startOk = start == 0
                                        || !Character.isLetterOrDigit(redacted.charAt(start-1));
                                    if (valid && startOk && endOk) {
                                        sb.append("[IP REDACTED]"); count++; i = end4; found = true;
                                    }
                                }
                            }
                        }
                    }
                }
                if (!found) { sb.append(redacted.charAt(i)); i++; }
            }
            if (count > 0) {
                detectedTypes.add("IP_ADDRESS"); detectedCounts.put("IP_ADDRESS", count);
                redacted = sb.toString();
            }
        }

        // --- ZIP CODE: 5 digits or 5-4 ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                boolean found = false;
                if (Character.isDigit(redacted.charAt(i))) {
                    int start = i, end = i;
                    while (end < redacted.length() && Character.isDigit(redacted.charAt(end))) end++;
                    int len = end - start;
                    boolean startOk = start == 0 || !Character.isLetterOrDigit(redacted.charAt(start-1));
                    if (len == 5 && end < redacted.length() && redacted.charAt(end) == '-'
                            && end+5 <= redacted.length()
                            && isDigits(redacted.substring(end+1, end+5))) {
                        boolean endOk = end+5 >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(end+5));
                        if (startOk && endOk) {
                            sb.append("[ZIP REDACTED]"); count++; i = end+5; found = true;
                        }
                    }
                    if (!found && len == 5) {
                        boolean endOk = end >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(end));
                        if (startOk && endOk) {
                            sb.append("[ZIP REDACTED]"); count++; i = end; found = true;
                        }
                    }
                    if (!found) { sb.append(redacted.substring(start, end)); i = end; }
                }
                if (!found && i < redacted.length() && !Character.isDigit(redacted.charAt(i))) {
                    sb.append(redacted.charAt(i)); i++;
                }
            }
            if (count > 0) {
                detectedTypes.add("ZIP_CODE"); detectedCounts.put("ZIP_CODE", count);
                redacted = sb.toString();
            }
        }

        // ============================================================
        // PART 2 - UNSTRUCTURED PII (OpenNLP NER)
        // ============================================================

        boolean nerDetected = false;
        String nerError = "";

        try {
            opennlp.tools.tokenize.SimpleTokenizer tokenizer =
                opennlp.tools.tokenize.SimpleTokenizer.INSTANCE;
            String[] tokens = tokenizer.tokenize(redacted);

            List<String[]> nerConfigs = new ArrayList<>();
            nerConfigs.add(new String[]{ "PERSON",       "models/en-ner-person.bin",       "[NAME REDACTED]"     });
            nerConfigs.add(new String[]{ "ORGANIZATION", "models/en-ner-organization.bin",  "[ORG REDACTED]"      });
            nerConfigs.add(new String[]{ "LOCATION",     "models/en-ner-location.bin",      "[LOCATION REDACTED]" });

            for (String[] cfg : nerConfigs) {
                String entityLabel = cfg[0];
                String modelPath   = cfg[1];
                String placeholder = cfg[2];

                java.io.InputStream modelStream =
                    getClass().getClassLoader().getResourceAsStream(modelPath);

                if (modelStream == null) {
                    nerError += "Model not found: " + modelPath + "; ";
                    continue;
                }

                opennlp.tools.namefind.TokenNameFinderModel model =
                    new opennlp.tools.namefind.TokenNameFinderModel(modelStream);
                modelStream.close();

                opennlp.tools.namefind.NameFinderME finder =
                    new opennlp.tools.namefind.NameFinderME(model);

                opennlp.tools.util.Span[] spans = finder.find(tokens);

                if (spans.length > 0) {
                    nerDetected = true;
                    detectedTypes.add(entityLabel);
                    detectedCounts.put(entityLabel, spans.length);

                    Set<Integer> redactIdx = new HashSet<>();
                    for (opennlp.tools.util.Span span : spans) {
                        for (int ti = span.getStart(); ti < span.getEnd(); ti++) {
                            redactIdx.add(ti);
                        }
                    }

                    StringBuilder nerSb = new StringBuilder();
                    for (int ti = 0; ti < tokens.length; ti++) {
                        if (redactIdx.contains(ti)) {
                            boolean isSpanStart = false;
                            for (opennlp.tools.util.Span span : spans) {
                                if (span.getStart() == ti) { isSpanStart = true; break; }
                            }
                            if (isSpanStart) {
                                if (nerSb.length() > 0) nerSb.append(" ");
                                nerSb.append(placeholder);
                            }
                        } else {
                            if (nerSb.length() > 0) nerSb.append(" ");
                            nerSb.append(tokens[ti]);
                        }
                    }
                    redacted = nerSb.toString();
                    tokens = tokenizer.tokenize(redacted);
                }

                finder.clearAdaptiveData();
            }

        } catch (Exception e) {
            nerError += e.getClass().getName() + ": " + e.getMessage();
        }

        boolean piiDetected = !detectedTypes.isEmpty();

        // --- Free-text audit trail ---
        List<String> reviewFields = new ArrayList<>();
        if (!freeTextFieldsRaw.trim().isEmpty()) {
            String[] fieldNames = freeTextFieldsRaw.split(",");
            for (String f : fieldNames) {
                String trimmed = f.trim();
                if (!trimmed.isEmpty()) reviewFields.add(trimmed);
            }
        }

        // ============================================================
        // BUILD detectionReport as Map (outputs as Document in Fusion)
        // ============================================================

        Map<String, Object> counts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : detectedCounts.entrySet()) {
            counts.put(entry.getKey(), entry.getValue());
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("piiDetected", piiDetected);
        report.put("nerDetected", nerDetected);
        report.put("detectedTypes", detectedTypes);
        report.put("counts", counts);
        report.put("freeTextFields", reviewFields);
        report.put("nerError", nerError);

        dataOut.put("piiDetected", piiDetected);
        dataOut.put("nerDetected", nerDetected);
        dataOut.put("detectionReport", report);
        dataOut.put("redactedText", redacted);

/* your code ends here */
        return dataOut;
    }

    private boolean isDigits(String s) {
        if (s == null || s.length() == 0) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}