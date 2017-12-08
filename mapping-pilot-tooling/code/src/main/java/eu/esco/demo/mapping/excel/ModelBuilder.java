package eu.esco.demo.mapping.excel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import eu.tenforce.commons.sem.jena.JenaUtils;

public class ModelBuilder {

  public static void main(String[] args) {
    Model model = new ModelBuilder(ModelFactory.createDefaultModel())
            .s("http://demo.com/natan",
               p(RDF.type, uri("http://some.other.com/rel/1")),
               p(RDFS.comment, uri("http://some.other.com/comment/111")))
            .s("http://demo.more.stuff.com/other/1",
               p(RDF.type, uri("http://unknown.type.com/type1")))
            .getModel();

    JenaUtils.write(model, System.out);

  }

  private final Model model;

  public ModelBuilder(Model model) {
    this.model = model;
  }

  public ModelBuilder s(String uri, PropertyBuilder... propertyBuilders) {
    Resource conceptResource = model.createResource(uri);
    for (PropertyBuilder propertyBuilder : propertyBuilders) {
      if (propertyBuilder.objects == null) continue;

      for (UriBuilder uriBuilder : propertyBuilder.objects) {
        model.add(conceptResource, propertyBuilder.property, model.getResource(uriBuilder.uri));
      }
    }
    return this;
  }

  public static PropertyBuilder p(Property property, UriBuilder... objects) {
    return new PropertyBuilder(property, objects);
  }

  public static UriBuilder uri(String uri) {
    return new UriBuilder(uri);
  }

  public Model getModel() {
    return model;
  }

  public static class PropertyBuilder {


    private final Property property;
    private final UriBuilder[] objects;

    public PropertyBuilder(Property property, UriBuilder[] objects) {
      this.property = property;
      this.objects = objects;
    }

    public PropertyBuilder s(String uri) {
      return new PropertyBuilder(null, null);
    }
  }

  public static class UriBuilder {
    private final String uri;

    public UriBuilder(String uri) {
      this.uri = uri;
    }
  }

//  public static Resource uri(String uri) {
//    return model.createResource(uri);
//  }

}
