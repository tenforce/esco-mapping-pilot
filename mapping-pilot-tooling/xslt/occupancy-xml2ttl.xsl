<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    xmlns:skos="http://www.w3.org/2004/02/skos/core#"
    exclude-result-prefixes="xs xd"
    version="2.0">
    <xd:doc scope="stylesheet">
        <xd:desc>
            <xd:p>Transform CZECH skill x,l to RDF for ESCO</xd:p>
            <xd:author>Horst Kucharczyk</xd:author>
            <xd:history>
                <xd:change version="0.01" date="2015-08-19">
                    <xd:desc>
                        <xd:p>creation</xd:p>
                        <xd:pre>java -jar D:\tools\SaxonHE9-5-1-3J\saxon9he.jar export-norm.xml -o:cz-occupancies.ttl -xsl:occupancy-xml2ttl.xsl</xd:pre>
                    </xd:desc>
                    <xd:contributor>Horst Kucharczyk</xd:contributor>
                </xd:change>
            </xd:history>
        </xd:desc>
    </xd:doc>
    
    <xsl:output encoding="UTF-8" method="text" indent="no"/>    
    <xsl:strip-space elements="*"/>
    
    <xsl:template match="/">
        <xsl:call-template name="prefix"/>
        <xsl:apply-templates/>        
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="text()"/>

<!-- terminated -->
<xsl:template match="occupancy">
<xsl:text disable-output-escaping="yes">&lt;http://pes.cz/abstr/concept/occupancy/</xsl:text><xsl:value-of select="@id_JP_Zaklad"/><xsl:text>> rdf:type skos:Concept ;
</xsl:text>
    <xsl:apply-templates select="professional-areas/NazevOdS"/><!-- for Honza to asses -->
    <xsl:apply-templates select="indentity/skos:prefLabel"/>
    <xsl:apply-templates select="altLabels"/>
    <xsl:apply-templates select="skos:definition"/>
    <xsl:apply-templates select="relatedTerms/relatedTerm"/>
    <xsl:apply-templates select="broaderTerms/broaderTerm"/>
    <xsl:apply-templates select="narrowerTerms/narrowerTerm"/>
    <xsl:apply-templates select="knowledges"/>
    <xsl:apply-templates select="skills"/>
    <xsl:apply-templates select="indentity/OccupationType"/>
<xsl:text>skos:inScheme czcs:Occupancies .
</xsl:text>
</xsl:template>
    
<xsl:template match="skos:definition">
<xsl:text>dcterms:description "</xsl:text><xsl:value-of select="."/><xsl:text>"@cs ;
</xsl:text>
</xsl:template>  
    
<xsl:template match="skos:prefLabel">
<xsl:text>skos:prefLabel "</xsl:text><xsl:value-of select="."/><xsl:text>"@cs ;
</xsl:text>
</xsl:template>
    
<xsl:template match="skos:altLabel">
<xsl:text>skos:altLabel "</xsl:text><xsl:value-of select="."/><xsl:text>" ;
</xsl:text>
</xsl:template>

<xsl:template match="relatedTerm">
<xsl:text>skos:related &lt;http://pes.cz/abstr/concept/occupancy/</xsl:text><xsl:value-of select="@id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
    
<xsl:template match="broaderTerm">
<xsl:text>skos:broader &lt;http://pes.cz/abstr/concept/occupancy/</xsl:text><xsl:value-of select="@id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
    
<xsl:template match="narrowerTerm">
<xsl:text>skos:narrower &lt;http://pes.cz/abstr/concept/occupancy/</xsl:text><xsl:value-of select="id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
    
<xsl:template match="knowledges/essential/knowledge">
<xsl:text>czcore:relatedEssentialCompetence &lt;http://pes.cz/abstr/concept/competence/knowledge/knowledge-professional/</xsl:text><xsl:value-of select="id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
<xsl:template match="knowledges/optional/knowledge">
<xsl:text>czcore:relatedOptionalCompetence &lt;http://pes.cz/abstr/concept/competence/knowledge/knowledge-professional/</xsl:text><xsl:value-of select="id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
    
<xsl:template match="skills[@class='professional']/essential/skill">
<xsl:text>czcore:relatedEssentialCompetence &lt;http://pes.cz/abstr/concept/competence/skill/skill-professional/</xsl:text><xsl:value-of select="id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
<xsl:template match="skills[@class='professional']/optional/skill">
<xsl:text>czcore:relatedOptionalCompetence &lt;http://pes.cz/abstr/concept/competence/skill/skill-professional/</xsl:text><xsl:value-of select="id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
    
<xsl:template match="skills[@class='soft']/skill">
<xsl:text>czcore:relatedEssentialCompetence &lt;http://pes.cz/abstr/concept/competence/skill/skill-soft/</xsl:text><xsl:value-of select="id"/><xsl:text>> ;
</xsl:text>
</xsl:template>
    
<xsl:template match="skills[@class='general']/skill">
<xsl:text>czcore:relatedOptionalCompetence &lt;http://pes.cz/abstr/concept/competence/skill/skill-general/</xsl:text><xsl:value-of select="id"/><xsl:text>> ;
</xsl:text>
</xsl:template>

<xsl:template match="OccupationType">
<xsl:text>czcore:type "</xsl:text><xsl:value-of select="."/><xsl:text>" ;
</xsl:text>
</xsl:template>

<xsl:template match="NazevOdS">
<xsl:text>czcore:inContext "</xsl:text><xsl:value-of select="."/><xsl:text>"@cs ;
</xsl:text>
</xsl:template>
    
    
<xsl:template name="prefix">
<xsl:text disable-output-escaping="yes">@prefix rdf:    &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix skos:     &lt;http://www.w3.org/2004/02/skos/core#> .
@prefix dc:       &lt;http://purl.org/dc/elements/1.1/> .
@prefix dcterms:  &lt;http://purl.org/dc/terms/> .
@prefix czcore:   &lt;http://pes.cz/core/> . 
@prefix czc:      &lt;http://pes.cz/abstr/concept/> .
@prefix czcs:     &lt;http://pes.cz/abstr/ConceptScheme/> .     
@prefix czco:     &lt;http://pes.cz/abstr/concept/competence/> .
@prefix czcok:    &lt;http://pes.cz/abstr/concept/competence/knowledge/> .
@prefix czcos:    &lt;http://pes.cz/abstr/concept/competence/skill/> .
&lt;http://pes.cz/abstr/ConceptScheme/Competences> rdf:type skos:ConceptScheme .
&lt;http://pes.cz/abstr/ConceptScheme/Occupancies> rdf:type skos:ConceptScheme .
&lt;http://pes.cz/abstr/concept/competence> rdf:type skos:Concept .
&lt;http://pes.cz/abstr/concept/competence> skos:inScheme czcs:Competences .
&lt;http://pes.cz/abstr/concept/competence/knowledge> rdf:type skos:Concept .
&lt;http://pes.cz/abstr/concept/competence/knowledge> skos:inScheme czcs:Competences .
&lt;http://pes.cz/abstr/concept/competence/knowledge> skos:broader czc:competence .
&lt;http://pes.cz/abstr/concept/competence/knowledge/knowledge-professional> rdf:type skos:Concept .
&lt;http://pes.cz/abstr/concept/competence/knowledge/knowledge-professional> skos:inScheme czcs:Competences .
&lt;http://pes.cz/abstr/concept/competence/knowledge/knowledge-professional> skos:broader czcok:knowledge .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-professional> rdf:type skos:Concept .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-professional> skos:inScheme czcs:Competences .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-professional> skos:broader czcos:competence .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-professional> dc:type "Skill" .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-soft> rdf:type skos:Concept .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-soft> skos:inScheme czcs:Competences .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-soft> skos:broader czcos:competence .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-soft> dc:type "Skill" .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-general> rdf:type skos:Concept .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-general> skos:inScheme czcs:Competences .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-general> skos:broader czcos:competence .
&lt;http://pes.cz/abstr/concept/competence/skill/skill-general> dc:type "Skill" .
</xsl:text>
    </xsl:template>
</xsl:stylesheet>