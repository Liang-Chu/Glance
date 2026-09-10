# Blip

An Android app that asks a backend **you** run for something to say, on a schedule you set, and shows
it as a notification that clears itself.

Built to feed [Even G2](https://www.evenrealities.com/) glasses through their notification feature,
but it is an ordinary Android notification and anything that reads those will see it.

## What it does

*Aspirational: this section describes the intended product. What is actually built is
[`docs/STATUS.md`](docs/STATUS.md).*

- Calls one backend you wrote, at your URL with your credential, on an interval you choose.
- Turns the reply into a notification that takes itself away again, so you never deal with it on the
  phone.
- Decides nothing about what the notification says — that is your backend's job, and the shape of the
  call between you is [`docs/CONTRACT.md`](docs/CONTRACT.md).
- Ships with no keys, no accounts, and no server of its own. A fresh install talks to nothing until
  you point it somewhere.

## Start here

- Writing the backend — [`docs/CONTRACT.md`](docs/CONTRACT.md)
- What exists and what runs today — [`docs/STATUS.md`](docs/STATUS.md)
- Why anything is built the way it is — [`docs/DESIGN.md`](docs/DESIGN.md)
- How to build, run, and review — [`docs/PROCESS.md`](docs/PROCESS.md)

Everything else starts at [`docs/README.md`](docs/README.md) — the only entrance to the docs.
