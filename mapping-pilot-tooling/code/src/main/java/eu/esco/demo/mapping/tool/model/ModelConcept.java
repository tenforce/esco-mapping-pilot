package eu.esco.demo.mapping.tool.model;

import java.util.List;

public interface ModelConcept {
	
	public String getUri();
	
	public String getPrefLabel();
	
	public List<String> getAltLabel();
	
	public String getDescription();
	
	public boolean hasDescription();
	
	public List<String> getBroader();
}
