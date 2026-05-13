import java.util.*;
// Please do not change pre-populated code.
public class PIIDetectionJavaService_piiDetectorSharedDP {

    public Map<String,Object> execute(Map<String,Object> dataIn) {
        Map<String,Object> dataOut = new HashMap<String, Object>();

/* Please start your code here */

        // --- Inputs ---
        String text = dataIn.get("textInput") != null
            ? dataIn.get("textInput").toString()
            : "";

        String freeTextFieldsRaw = dataIn.get("freeTextFields") != null
            ? dataIn.get("freeTextFields").toString()
            : "";

        List<String> detectedTypes = new ArrayList<>();
        Map<String, Integer> detectedCounts = new LinkedHashMap<>();
        String redacted = text;

        // -------------------------------------------------------
        // HELPER: check if char is a digit
        // -------------------------------------------------------

        // --- EMAIL detection ---
        // Look for @ with non-space chars on both sides and a dot after @
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                int atIdx = redacted.indexOf('@', i);
                if (atIdx < 0) {
                    sb.append(redacted.substring(i));
                    break;
                }
                // scan left for start of local part
                int start = atIdx - 1;
                while (start >= 0) {
                    char c = redacted.charAt(start);
                    if (Character.isLetterOrDigit(c) || c == '.' || c == '_'
                            || c == '%' || c == '+' || c == '-') {
                        start--;
                    } else {
                        break;
                    }
                }
                start++;
                // scan right for domain
                int end = atIdx + 1;
                while (end < redacted.length()) {
                    char c = redacted.charAt(end);
                    if (Character.isLetterOrDigit(c) || c == '.' || c == '-') {
                        end++;
                    } else {
                        break;
                    }
                }
                // must have a dot in domain portion and local part non-empty
                String domain = redacted.substring(atIdx + 1, end);
                boolean validLocal = atIdx > start;
                boolean validDomain = domain.indexOf('.') >= 0 && domain.length() > 2;
                if (validLocal && validDomain) {
                    sb.append(redacted.substring(i, start));
                    sb.append("[EMAIL REDACTED]");
                    count++;
                    i = end;
                } else {
                    sb.append(redacted.substring(i, atIdx + 1));
                    i = atIdx + 1;
                }
            }
            if (count > 0) {
                detectedTypes.add("EMAIL");
                detectedCounts.put("EMAIL", count);
                redacted = sb.toString();
            }
        }

        // --- SSN detection: NNN-NN-NNNN or NNNNNNNNN ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                // need at least 11 chars for dashed format or 9 for plain
                if (i + 9 > redacted.length()) {
                    sb.append(redacted.substring(i));
                    break;
                }
                // check word boundary
                if (i > 0 && Character.isLetterOrDigit(redacted.charAt(i - 1))) {
                    sb.append(redacted.charAt(i));
                    i++;
                    continue;
                }
                boolean found = false;
                // dashed: DDD-DD-DDDD
                if (i + 11 <= redacted.length()) {
                    String candidate = redacted.substring(i, i + 11);
                    if (isDigits(candidate.substring(0, 3))
                            && candidate.charAt(3) == '-'
                            && isDigits(candidate.substring(4, 6))
                            && candidate.charAt(6) == '-'
                            && isDigits(candidate.substring(7, 11))) {
                        boolean endOk = i + 11 >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(i + 11));
                        if (endOk) {
                            sb.append("[SSN REDACTED]");
                            count++;
                            i += 11;
                            found = true;
                        }
                    }
                }
                // plain 9 digits
                if (!found && i + 9 <= redacted.length()) {
                    String candidate = redacted.substring(i, i + 9);
                    if (isDigits(candidate)) {
                        boolean endOk = i + 9 >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(i + 9));
                        if (endOk) {
                            sb.append("[SSN REDACTED]");
                            count++;
                            i += 9;
                            found = true;
                        }
                    }
                }
                if (!found) {
                    sb.append(redacted.charAt(i));
                    i++;
                }
            }
            if (count > 0) {
                detectedTypes.add("SSN");
                detectedCounts.put("SSN", count);
                redacted = sb.toString();
            }
        }

        // --- PHONE detection: sequences matching (NNN) NNN-NNNN or NNN-NNN-NNNN ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                boolean found = false;
                // skip +1 or 1 prefix
                int start = i;
                if (i < redacted.length() && redacted.charAt(i) == '+') i++;
                if (i < redacted.length() && redacted.charAt(i) == '1'
                        && i + 1 < redacted.length()
                        && !Character.isDigit(redacted.charAt(i + 1))) {
                    i++;
                    if (i < redacted.length() && (redacted.charAt(i) == ' '
                            || redacted.charAt(i) == '-')) i++;
                }

                // format: (NNN) NNN-NNNN
                if (i + 13 <= redacted.length() && redacted.charAt(i) == '(') {
                    String cand = redacted.substring(i, i + 13);
                    if (cand.charAt(0) == '('
                            && isDigits(cand.substring(1, 4))
                            && cand.charAt(4) == ')'
                            && cand.charAt(5) == ' '
                            && isDigits(cand.substring(6, 9))
                            && cand.charAt(9) == '-'
                            && isDigits(cand.substring(10, 14 > cand.length() ? cand.length() : 14))) {
                        // re-check with correct length
                    }
                    if (cand.charAt(0) == '(' && isDigits(cand.substring(1,4))
                            && cand.charAt(4) == ')' && cand.charAt(5) == ' '
                            && isDigits(cand.substring(6,9)) && cand.charAt(9) == '-'
                            && i + 14 <= redacted.length()
                            && isDigits(redacted.substring(i+10, i+14))) {
                        boolean endOk = i + 14 >= redacted.length()
                            || !Character.isDigit(redacted.charAt(i + 14));
                        if (endOk) {
                            sb.append(redacted.substring(start, i));
                            sb.append("[PHONE REDACTED]");
                            count++;
                            i += 14;
                            found = true;
                        }
                    }
                }

                // format: NNN-NNN-NNNN
                if (!found && i + 12 <= redacted.length()) {
                    if (isDigits(redacted.substring(i, i+3))
                            && redacted.charAt(i+3) == '-'
                            && isDigits(redacted.substring(i+4, i+7))
                            && redacted.charAt(i+7) == '-'
                            && isDigits(redacted.substring(i+8, i+12))) {
                        boolean startOk = i == 0
                            || !Character.isLetterOrDigit(redacted.charAt(i-1));
                        boolean endOk = i + 12 >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(i+12));
                        if (startOk && endOk) {
                            sb.append(redacted.substring(start, i));
                            sb.append("[PHONE REDACTED]");
                            count++;
                            i += 12;
                            found = true;
                        }
                    }
                }

                if (!found) {
                    i = start; // reset prefix skip
                    sb.append(redacted.charAt(i));
                    i++;
                }
            }
            if (count > 0) {
                detectedTypes.add("PHONE");
                detectedCounts.put("PHONE", count);
                redacted = sb.toString();
            }
        }

        // --- CREDIT CARD detection: 13-16 contiguous digits at word boundary ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                if (!Character.isDigit(redacted.charAt(i))) {
                    sb.append(redacted.charAt(i));
                    i++;
                    continue;
                }
                // count consecutive digits
                int start = i;
                while (i < redacted.length() && Character.isDigit(redacted.charAt(i))) i++;
                int len = i - start;
                boolean startOk = start == 0 || !Character.isLetterOrDigit(redacted.charAt(start-1));
                boolean endOk = i >= redacted.length() || !Character.isLetterOrDigit(redacted.charAt(i));
                if (len >= 13 && len <= 16 && startOk && endOk) {
                    sb.append("[CREDIT CARD REDACTED]");
                    count++;
                } else {
                    sb.append(redacted.substring(start, i));
                }
            }
            if (count > 0) {
                detectedTypes.add("CREDIT_CARD");
                detectedCounts.put("CREDIT_CARD", count);
                redacted = sb.toString();
            }
        }

        // --- IP ADDRESS detection: N.N.N.N where each octet 0-255 ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                boolean found = false;
                if (Character.isDigit(redacted.charAt(i))) {
                    int start = i;
                    // read octet1
                    int end1 = i;
                    while (end1 < redacted.length() && Character.isDigit(redacted.charAt(end1))) end1++;
                    if (end1 < redacted.length() && redacted.charAt(end1) == '.'
                            && end1 - i <= 3) {
                        int o1 = Integer.parseInt(redacted.substring(i, end1));
                        int j = end1 + 1;
                        // octet2
                        int end2 = j;
                        while (end2 < redacted.length() && Character.isDigit(redacted.charAt(end2))) end2++;
                        if (end2 < redacted.length() && redacted.charAt(end2) == '.'
                                && end2 - j <= 3 && end2 > j) {
                            int o2 = Integer.parseInt(redacted.substring(j, end2));
                            int k = end2 + 1;
                            // octet3
                            int end3 = k;
                            while (end3 < redacted.length() && Character.isDigit(redacted.charAt(end3))) end3++;
                            if (end3 < redacted.length() && redacted.charAt(end3) == '.'
                                    && end3 - k <= 3 && end3 > k) {
                                int o3 = Integer.parseInt(redacted.substring(k, end3));
                                int l = end3 + 1;
                                // octet4
                                int end4 = l;
                                while (end4 < redacted.length() && Character.isDigit(redacted.charAt(end4))) end4++;
                                if (end4 > l && end4 - l <= 3) {
                                    int o4 = Integer.parseInt(redacted.substring(l, end4));
                                    boolean valid = o1 <= 255 && o2 <= 255 && o3 <= 255 && o4 <= 255;
                                    boolean endOk = end4 >= redacted.length()
                                        || !Character.isLetterOrDigit(redacted.charAt(end4));
                                    boolean startOk = start == 0
                                        || !Character.isLetterOrDigit(redacted.charAt(start-1));
                                    if (valid && startOk && endOk) {
                                        sb.append("[IP REDACTED]");
                                        count++;
                                        i = end4;
                                        found = true;
                                    }
                                }
                            }
                        }
                    }
                }
                if (!found) {
                    sb.append(redacted.charAt(i));
                    i++;
                }
            }
            if (count > 0) {
                detectedTypes.add("IP_ADDRESS");
                detectedCounts.put("IP_ADDRESS", count);
                redacted = sb.toString();
            }
        }

        // --- ZIP CODE detection: exactly 5 digits, or 5-4 with hyphen ---
        {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < redacted.length()) {
                boolean found = false;
                if (Character.isDigit(redacted.charAt(i))) {
                    int start = i;
                    int end = i;
                    while (end < redacted.length() && Character.isDigit(redacted.charAt(end))) end++;
                    int len = end - start;
                    boolean startOk = start == 0 || !Character.isLetterOrDigit(redacted.charAt(start-1));
                    // 5-digit zip+4
                    if (len == 5 && end < redacted.length() && redacted.charAt(end) == '-'
                            && end + 5 <= redacted.length()
                            && isDigits(redacted.substring(end+1, end+5))) {
                        boolean endOk = end + 5 >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(end+5));
                        if (startOk && endOk) {
                            sb.append("[ZIP REDACTED]");
                            count++;
                            i = end + 5;
                            found = true;
                        }
                    }
                    // plain 5-digit zip
                    if (!found && len == 5) {
                        boolean endOk = end >= redacted.length()
                            || !Character.isLetterOrDigit(redacted.charAt(end));
                        if (startOk && endOk) {
                            sb.append("[ZIP REDACTED]");
                            count++;
                            i = end;
                            found = true;
                        }
                    }
                    if (!found) {
                        sb.append(redacted.substring(start, end));
                        i = end;
                    }
                }
                if (!found && i < redacted.length() && !Character.isDigit(redacted.charAt(i))) {
                    sb.append(redacted.charAt(i));
                    i++;
                }
            }
            if (count > 0) {
                detectedTypes.add("ZIP_CODE");
                detectedCounts.put("ZIP_CODE", count);
                redacted = sb.toString();
            }
        }

        boolean piiDetected = !detectedTypes.isEmpty();

        // --- Free-text manual review flag ---
        boolean requiresManualReview = false;
        List<String> reviewFields = new ArrayList<>();

        if (!freeTextFieldsRaw.trim().isEmpty()) {
            String[] fieldNames = freeTextFieldsRaw.split(",");
            for (String f : fieldNames) {
                String trimmed = f.trim();
                if (!trimmed.isEmpty()) {
                    reviewFields.add(trimmed);
                    requiresManualReview = true;
                }
            }
        }

        // --- Build JSON report ---
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"piiDetected\":").append(piiDetected).append(",");
        json.append("\"requiresManualReview\":").append(requiresManualReview).append(",");

        json.append("\"detectedTypes\":[");
        for (int i = 0; i < detectedTypes.size(); i++) {
            json.append("\"").append(detectedTypes.get(i)).append("\"");
            if (i < detectedTypes.size() - 1) json.append(",");
        }
        json.append("],");

        json.append("\"counts\":{");
        int idx = 0;
        for (Map.Entry<String, Integer> entry : detectedCounts.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            if (idx < detectedCounts.size() - 1) json.append(",");
            idx++;
        }
        json.append("},");

        json.append("\"manualReviewFields\":[");
        for (int i = 0; i < reviewFields.size(); i++) {
            json.append("\"").append(reviewFields.get(i)).append("\"");
            if (i < reviewFields.size() - 1) json.append(",");
        }
        json.append("],");

        json.append("\"note\":\"")
            .append(requiresManualReview
                ? "Free-text fields flagged - regex cannot detect names or orgs. Human review required."
                : "No free-text fields flagged.")
            .append("\"");

        json.append("}");

        dataOut.put("piiDetected", piiDetected);
        dataOut.put("detectionReport", json.toString());
        dataOut.put("requiresManualReview", requiresManualReview);
        dataOut.put("redactedText", redacted);

/* your code ends here */
        return dataOut;
    }

    // --- Helper: returns true if every character in s is a digit ---
    private boolean isDigits(String s) {
        if (s == null || s.length() == 0) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}