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
                    </xd:desc>
                    <xd:contributor>Horst Kucharczyk</xd:contributor>
                </xd:change>
            </xd:history>
        </xd:desc>
    </xd:doc>
    
    <xsl:output encoding="UTF-8" method="text" indent="no"/>
    
    <xsl:template match="/">
        <xsl:call-template name="prefix"/>
        <xsl:apply-templates/>        
    </xsl:template>
    
    <xsl:template match="text()"/>
    
<xsl:template match="knowledge">
<xsl:text disable-output-escaping="yes">&lt;</xsl:text><xsl:value-of select="id"></xsl:value-of><xsl:text>> rdf:type skos:Concept ;
</xsl:text>
<xsl:text>skos:inScheme czcs:Competences ;
</xsl:text>    
<xsl:text>skos:broader czco:knowledge ;
</xsl:text>
<xsl:text>czcore:type "knowledge-professional" ;
</xsl:text>
<xsl:text>skos:prefLabel "</xsl:text><xsl:value-of select="skos:prefLabel"/><xsl:text>"@cs .
</xsl:text>
    
</xsl:template>

<xsl:template match="skills-professional/skill">
<xsl:call-template name="skill"/>
<xsl:text>czcore:type "skill-professional" .
</xsl:text>
</xsl:template>

<xsl:template match="skills-soft/skill">
<xsl:call-template name="skill"/>
<xsl:text>czcore:type "skill-soft" .
</xsl:text>
</xsl:template>

<xsl:template match="skills-general/skill">
<xsl:call-template name="skill"/>
<xsl:text>czcore:type "skill-general" .
</xsl:text>
</xsl:template>

<!-- unterminated -->
<xsl:template name="skill">
<xsl:text disable-output-escaping="yes">&lt;</xsl:text><xsl:value-of select="id"></xsl:value-of><xsl:text>> rdf:type skos:Concept ;
</xsl:text>
<xsl:text>skos:inScheme czcs:Competences ;
</xsl:text>    
<xsl:text>skos:broader czco:knowledge ;
</xsl:text>    
<xsl:text>skos:prefLabel "</xsl:text><xsl:value-of select="skos:prefLabel"/><xsl:text>"@cs ;
</xsl:text>
</xsl:template>

<xsl:template name="prefix">
<xsl:text disable-output-escaping="yes">@prefix rdf:    &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix skos:     &lt;http://www.w3.org/2004/02/skos/core#> .
@prefix dc:       &lt;http://purl.org/dc/elements/1.1/> .
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