# Example backend

The smallest thing that satisfies [`../../docs/CONTRACT.md`](../../docs/CONTRACT.md). It returns
`content.json`, verbatim, and does nothing else. Python 3.8+, no dependencies.

```
python server.py
python server.py --port 9000
python server.py --token hunter2    # require Authorization: Bearer hunter2
```

It prints every field Glance sends and whether what it is about to return will be accepted or
refused, which is usually enough to find the problem without touching the phone.

Edit `content.json` between checks and the next one picks it up — no restart.

## Testing the rules by editing one file

The file is sent verbatim, so it exercises the contract including the parts that must fail:

| `content.json` | Glance should |
| --- | --- |
| `{"title": "Kotlin", "text": "..."}` | show it |
| *(empty file)* | show nothing, and not complain — that is `204` |
| `{"text": "..."}` | refuse: no title |
| `{"title": "T", "text": ""}` | refuse: empty text |
| text longer than the max length | refuse, **not** truncate |
| `hello` | refuse: not JSON |
| *(delete the file)* | complain once, then stay quiet until it is back |

A failure notice appears **once**, then Glance goes quiet until a check succeeds. That is deliberate.
Put the file back and wait for the next check to see it recover.

## Reaching it from the phone

The phone must be on the same network. The server prints the addresses it answers on at startup —
**these change when the machine changes network**, which is the most common reason a working setup
stops working. Check there first.

This is a toy for testing. It has no authentication worth the name, serves one file, and should not
be exposed to the internet.
