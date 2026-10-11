# Command-line classification

`CommandLineRunnerV1` is the Spring CLI entry point for the same classification, validation, and XLSX-writing flow used by the upload UI. It is disabled unless `xls2llm.cli.enabled=true`.

Build the executable jar:

```powershell
./mvnw.cmd -DskipTests package
```

Run 100 rows starting at line 1:

```powershell
$cli = @(
  '--spring.main.web-application-type=none',
  '--xls2llm.cli.enabled=true',
  '--xls2llm.cli.input=src/main/resources/xls/test1-simple.xlsx',
  '--xls2llm.cli.output=target/test1-gpt-4.1.xlsx',
  '--xls2llm.cli.start-line=1',
  '--xls2llm.cli.row-limit=100',
  '--llm.model=gpt-4.1'
)
java -jar target/xls2llm-0.0.3.jar $cli
```

Repeat with another model and output path, for example `--llm.model=gpt-5-mini` and `--xls2llm.cli.output=target/test1-gpt-5-mini.xlsx`. Both runs use the same configured OpenAI URL/key/org/project and methodology profile as the UI. CLI options override values from Spring configuration. `start-line` is one-based; omitting `row-limit` processes the remaining rows. If `output` is omitted, the runner creates a `-classified.xlsx` file beside the input.

The CLI prints a progress percentage after each completed batch, followed by the final local-validation success percentage.

If a response omits a requested `commentId`, the run continues: received rows are written, missing rows remain blank with red classification cells, and the `LLM result status` column records `MISSING_LLM_RESULT`. A duplicate response marks that row `DUPLICATE_LLM_RESULT`. If an API request fails, the failed batch and remaining rows are marked missing and the workbook is still finalized with run metadata.
