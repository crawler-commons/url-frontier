# URL Frontier documentation

The URL Frontier documentation is written in Markdown and built with
[Zensical](https://zensical.org/).

## Local preview

Create and activate a Python virtual environment, then install Zensical:

    python -m pip install zensical

From the `docs` directory, start the local documentation server:

    zensical serve

Then open:

    http://localhost:8000

## Build the documentation

To generate the static site:

    zensical build --clean

The generated site is written to:

    docs/site/

The generated site directory is ignored by Git and should not be committed.
