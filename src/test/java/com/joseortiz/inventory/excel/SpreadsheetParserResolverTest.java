package com.joseortiz.inventory.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joseortiz.inventory.TestProperties;
import com.joseortiz.inventory.error.InvalidFileException;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpreadsheetParserResolverTest {

  private final XlsxSpreadsheetParser xlsx = new XlsxSpreadsheetParser(TestProperties.defaults());
  private final SpreadsheetParserResolver resolver =
      new SpreadsheetParserResolver(List.of(xlsx), TestProperties.defaults());

  @Test
  void resolvesByExtensionCaseInsensitively() {
    assertThat(resolver.resolve("Inventory.XLSX")).isSameAs(xlsx);
  }

  @Test
  void rejectsUnsupportedExtension() {
    assertThatThrownBy(() -> resolver.resolve("inventory.csv"))
        .isInstanceOfSatisfying(
            InvalidFileException.class,
            ex ->
                assertThat(ex.getDetails().get(0).info())
                    .containsEntry("fileName", "inventory.csv")
                    .containsEntry("accepted", "xlsx"));
  }

  @Test
  void rejectsMissingExtension() {
    assertThatThrownBy(() -> resolver.resolve("inventory")).isInstanceOf(InvalidFileException.class);
    assertThat(SpreadsheetParserResolver.extensionOf(null)).isEmpty();
  }
}
