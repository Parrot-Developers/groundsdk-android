def _setup_codeblock_substitutions(app):
    for ext in [
        'sphinx-prompt',
        'sphinx_substitution_extensions',
    ]:
        app.setup_extension(ext)

    # list of interpretable substitutions
    app.config.substitutions = [
        ('|version|', app.config.version),
    ]

def setup(app):
    _setup_codeblock_substitutions(app)

