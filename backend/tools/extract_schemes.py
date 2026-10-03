"""
One-shot extractor: SchemeRepository.kt -> schemes.json

Parses the hardcoded Kotlin catalog into JSON that the seed script loads into
Postgres. Run by hand; its output is committed so the seed is reviewable and
reproducible without re-running the parser.

This is a throwaway migration tool, not production code. It is deliberately
strict: anything it cannot parse unambiguously raises rather than guessing,
because a silently mis-parsed eligibility rule produces plausible wrong
verdicts rather than an error.

Usage:
    python backend/tools/extract_schemes.py
"""

import io
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, '..', '..'))
SRC = os.path.join(REPO, 'app', 'src', 'main', 'java', 'com', 'example',
                   'data', 'repository', 'SchemeRepository.kt')
OUT = os.path.join(HERE, 'schemes.json')

# Kotlin defaults from Scheme.kt. Schemes that omit these fields inherit them,
# so the extracted JSON must carry the same values or the DB round-trip would
# silently differ from what the app shows today.
DEFAULT_OFFICIAL_SOURCE_LABEL = u'Official Government Source — Prototype Reference'
DEFAULT_LAST_VERIFIED_DATE = u'August 2026'
DEFAULT_APPLICATION_METHOD = u'Online via Official Portal / Common Service Center (CSC)'
DEFAULT_APPLICATION_STEPS = [
    u'Register on the official government portal or visit your nearest Common Service Center (CSC).',
    u'Upload/submit mandatory identity credentials and category certificates.',
    u'Submit the application and receive your official acknowledgment receipt.',
    u'Track application status online using your reference application ID.',
]

# Kotlin default for SchemeDocument.
DEFAULT_DOC_MANDATORY = True
DEFAULT_DOC_STAGE = u'Application Submission'
DEFAULT_DOC_TIP = u'Keep official digital or physical copy ready'


class ParseError(Exception):
    pass


def strip_comments(text):
    """Remove // line comments that fall outside string literals."""
    out = []
    i = 0
    n = len(text)
    in_str = False
    while i < n:
        c = text[i]
        if in_str:
            if c == '\\':
                out.append(text[i:i + 2])
                i += 2
                continue
            if c == '"':
                in_str = False
            out.append(c)
            i += 1
            continue
        if c == '"':
            in_str = True
            out.append(c)
            i += 1
            continue
        if c == '/' and i + 1 < n and text[i + 1] == '/':
            while i < n and text[i] != '\n':
                i += 1
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def split_top_level(body):
    """Split a Kotlin argument list on commas at nesting depth zero."""
    parts = []
    depth = 0
    in_str = False
    cur = []
    i = 0
    n = len(body)
    while i < n:
        c = body[i]
        if in_str:
            if c == '\\':
                cur.append(body[i:i + 2])
                i += 2
                continue
            if c == '"':
                in_str = False
            cur.append(c)
            i += 1
            continue
        if c == '"':
            in_str = True
            cur.append(c)
            i += 1
            continue
        if c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
        if c == ',' and depth == 0:
            parts.append(''.join(cur))
            cur = []
            i += 1
            continue
        cur.append(c)
        i += 1
    tail = ''.join(cur).strip()
    if tail:
        parts.append(tail)
    return [p.strip() for p in parts if p.strip()]


def string_spans(text):
    """Return (start, end) spans of every double-quoted string literal."""
    spans = []
    i = 0
    n = len(text)
    while i < n:
        if text[i] == '"':
            start = i
            i += 1
            while i < n:
                if text[i] == '\\':
                    i += 2
                    continue
                if text[i] == '"':
                    break
                i += 1
            spans.append((start, i))
        i += 1
    return spans


def find_calls(text, name):
    """
    Yield the argument-list body of each `name(...)` call, balanced.

    Identifiers inside string literals are skipped: scheme names legitimately
    contain parenthesised acronyms such as "... Scheme (DDRS)", which would
    otherwise be mistaken for a call.
    """
    results = []
    spans = string_spans(text)

    def inside_string(pos):
        for a, b in spans:
            if a <= pos <= b:
                return True
            if a > pos:
                break
        return False

    for m in re.finditer(r'\b%s\s*\(' % re.escape(name), text):
        if inside_string(m.start()):
            continue
        start = m.end()
        depth = 1
        in_str = False
        i = start
        while i < len(text) and depth > 0:
            c = text[i]
            if in_str:
                if c == '\\':
                    i += 2
                    continue
                if c == '"':
                    in_str = False
            elif c == '"':
                in_str = True
            elif c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
            i += 1
        if depth != 0:
            raise ParseError('Unbalanced parentheses in %s(' % name)
        results.append((m.start(), text[start:i - 1]))
    return results


def parse_args(body):
    """Parse `name = value` pairs into a dict of raw value strings."""
    args = {}
    for part in split_top_level(body):
        m = re.match(r'^(\w+)\s*=\s*(.*)$', part, re.S)
        if not m:
            raise ParseError('Unparsed argument: %r' % part[:80])
        args[m.group(1)] = m.group(2).strip()
    return args


def unquote(raw):
    """Decode a Kotlin string literal, including escapes."""
    raw = raw.strip()
    if not (raw.startswith('"') and raw.endswith('"')):
        raise ParseError('Expected a string literal, got: %r' % raw[:80])
    inner = raw[1:-1]
    out = []
    i = 0
    while i < len(inner):
        c = inner[i]
        if c == '\\' and i + 1 < len(inner):
            nxt = inner[i + 1]
            out.append({'n': '\n', 't': '\t', '"': '"', '\\': '\\',
                        'r': '\r', '$': '$'}.get(nxt, nxt))
            i += 2
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def parse_string_list(raw):
    """Parse listOf("a", "b") into a list of decoded strings."""
    raw = raw.strip()
    calls = find_calls(raw, 'listOf')
    if not calls:
        raise ParseError('Expected listOf(...), got: %r' % raw[:80])
    return [unquote(p) for p in split_top_level(calls[0][1])]


def parse_target_value(raw):
    """
    Decode a criterion targetValue into a native JSON type.

    The type matters: EligibilityEngine reads booleans as `as? Boolean ?: true`,
    so a boolean arriving as a string silently inverts the rule. Each shape is
    recognised explicitly and anything unrecognised raises.
    """
    raw = raw.strip()
    if raw == 'true':
        return True
    if raw == 'false':
        return False
    if raw.startswith('listOf'):
        return parse_string_list(raw)
    if raw.startswith('"'):
        return unquote(raw)
    # Numeric literal, possibly with a Long suffix and underscore separators.
    m = re.match(r'^-?[\d_]+[Ll]?$', raw)
    if m:
        return int(raw.rstrip('Ll').replace('_', ''))
    raise ParseError('Unrecognised targetValue: %r' % raw[:80])


def main():
    if not os.path.exists(SRC):
        sys.exit('Source not found: %s' % SRC)

    text = strip_comments(io.open(SRC, encoding='utf-8').read())

    schemes = []
    for _pos, body in find_calls(text, 'Scheme'):
        a = parse_args(body)

        criteria = []
        for _cpos, cbody in find_calls(a.get('criteria', ''), 'EligibilityCriterion'):
            c = parse_args(cbody)
            cond = c['conditionType'].split('.')[-1].strip()
            criteria.append({
                'id': unquote(c['id']),
                'title': unquote(c['title']),
                'conditionType': cond,
                'targetValue': parse_target_value(c['targetValue']),
                'requirementDisplay': unquote(c['requirementDisplay']),
                'explanationNote': unquote(c['explanationNote']),
                'whyWeAskReason': unquote(c['whyWeAskReason']),
            })

        documents = []
        for _dpos, dbody in find_calls(a.get('requiredDocuments', ''), 'SchemeDocument'):
            d = parse_args(dbody)
            documents.append({
                'id': unquote(d['id']),
                'name': unquote(d['name']),
                'isMandatoryForEligibility': (
                    d['isMandatoryForEligibility'].strip() == 'true'
                    if 'isMandatoryForEligibility' in d else DEFAULT_DOC_MANDATORY
                ),
                'stage': unquote(d['stage']) if 'stage' in d else DEFAULT_DOC_STAGE,
                'tip': unquote(d['tip']) if 'tip' in d else DEFAULT_DOC_TIP,
            })

        schemes.append({
            'id': unquote(a['id']),
            'name': unquote(a['name']),
            'shortName': unquote(a['shortName']),
            'tamilName': unquote(a['tamilName']),
            'hindiName': unquote(a['hindiName']),
            'category': a['category'].split('.')[-1].strip(),
            'department': unquote(a['department']),
            'description': unquote(a['description']),
            'benefitHighlight': unquote(a['benefitHighlight']),
            'detailedBenefits': parse_string_list(a['detailedBenefits']),
            'criteria': criteria,
            'requiredDocuments': documents,
            'officialSourceLabel': (
                unquote(a['officialSourceLabel'])
                if 'officialSourceLabel' in a else DEFAULT_OFFICIAL_SOURCE_LABEL
            ),
            'sourceUrl': unquote(a['sourceUrl']),
            'lastVerifiedDate': (
                unquote(a['lastVerifiedDate'])
                if 'lastVerifiedDate' in a else DEFAULT_LAST_VERIFIED_DATE
            ),
            'applicationMethod': (
                unquote(a['applicationMethod'])
                if 'applicationMethod' in a else DEFAULT_APPLICATION_METHOD
            ),
            'applicationSteps': (
                parse_string_list(a['applicationSteps'])
                if 'applicationSteps' in a else list(DEFAULT_APPLICATION_STEPS)
            ),
        })

    # Integrity checks. Any failure here means the seed would be wrong.
    ids = [s['id'] for s in schemes]
    if len(ids) != len(set(ids)):
        raise ParseError('Duplicate scheme ids')
    for s in schemes:
        cids = [c['id'] for c in s['criteria']]
        if len(cids) != len(set(cids)):
            raise ParseError('Duplicate criterion ids in %s' % s['id'])
        dids = [d['id'] for d in s['requiredDocuments']]
        if len(dids) != len(set(dids)):
            raise ParseError('Duplicate document ids in %s: %s' % (s['id'], dids))

    io.open(OUT, 'w', encoding='utf-8', newline='\n').write(
        json.dumps(schemes, ensure_ascii=False, indent=2, sort_keys=True) + '\n'
    )

    print('schemes:   %d' % len(schemes))
    print('criteria:  %d' % sum(len(s['criteria']) for s in schemes))
    print('documents: %d' % sum(len(s['requiredDocuments']) for s in schemes))
    print('wrote %s' % OUT)


if __name__ == '__main__':
    main()
