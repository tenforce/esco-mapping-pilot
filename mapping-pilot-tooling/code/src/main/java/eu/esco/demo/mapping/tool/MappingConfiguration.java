package eu.esco.demo.mapping.tool;

import org.slf4j.helpers.MessageFormatter;

public class MappingConfiguration {

  public static MappingConfiguration fromTemplate(String language, String searchServerTemplate, String searchType) {
    return new MappingConfiguration(language,
                                    getSearchServer(language, searchServerTemplate),
                                    searchType);
  }

  public static String getSearchServer(String language, String searchServerTemplate) {
    return MessageFormatter.format(searchServerTemplate, language).getMessage();
  }

  private String language;
  private String searchServer;
  private String searchType;

  public MappingConfiguration() {
  }

  public MappingConfiguration(String language, String searchServer, String searchType) {
    this.language = language;
    this.searchServer = searchServer;
    this.searchType = searchType;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getSearchServer() {
    return searchServer;
  }

  public void setSearchServer(String searchServer) {
    this.searchServer = searchServer;
  }

  public String getSearchType() {
    return searchType;
  }

  public void setSearchType(String searchType) {
    this.searchType = searchType;
  }

  @Override
  public String toString() {
    return "MappingConfiguration{" +
            "language='" + language + '\'' +
            ", searchServer='" + searchServer + '\'' +
            ", searchType='" + searchType + '\'' +
            '}';
  }
}
