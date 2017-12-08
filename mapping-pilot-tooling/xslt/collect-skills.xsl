<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:skos="http://www.w3.org/2004/02/skos/core#"
    xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    exclude-result-prefixes="#all"
    version="2.0">
    <xd:doc scope="stylesheet">
        <xd:desc>
            <xd:p>collect skills and knowledges</xd:p>
            <xd:author>Horst Kucharczyk</xd:author>
            <xd:history>
                <xd:change version="0.01" date="2015-08-28">
                    <xd:desc>
                        <xd:p>creation</xd:p>
                    </xd:desc>
                    <xd:contributor>Horst Kucharczyk</xd:contributor>
                </xd:change>
            </xd:history>
            <xd:pre>java -jar D:\tools\SaxonHE9-5-1-3J\saxon9he.jar exportPozic-corrected.xml -o:export-norm.xml -xsl:normalise-src.xsl</xd:pre>
            <xd:pre>java -jar D:\tools\SaxonHE9-5-1-3J\saxon9he.jar export-norm.xml -o:cz-competences.xml -xsl:collect-skills.xsl</xd:pre>
        </xd:desc>
       
    </xd:doc>
    
    <xsl:output encoding="UTF-8" method="xml" indent="yes"/>    
    <xsl:strip-space elements="*"/>
    
    <xsl:template match="/">
        <xsl:apply-templates/>        
    </xsl:template>
       
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates/>   
        </xsl:copy>        
    </xsl:template>
    
    <xsl:template match="occupancies">
        <competences>
            <xsl:namespace name="skos">http://www.w3.org/2004/02/skos/core#</xsl:namespace>
            <knowlewdges>
                <xsl:for-each-group select="//knowledge" group-by="id">
                    <xsl:sort select="id"/>
                    <xsl:apply-templates select="current-group()[1]"><xsl:with-param name="pre" select="'http://pes.cz/abstr/concept/competence/knowledge/knowledge-professional/'" tunnel="yes"/></xsl:apply-templates>
                </xsl:for-each-group>
            </knowlewdges>
            <skills-professional>
                <xsl:for-each-group select="//skills[@class='professional']/descendant::skill" group-by="id">
                    <xsl:sort select="id"/>
                    <xsl:apply-templates select="current-group()[1]"><xsl:with-param name="pre" select="'http://pes.cz/abstr/concept/competence/skill/skill-professional/'" tunnel="yes"/></xsl:apply-templates>
                </xsl:for-each-group>
            </skills-professional>
            <skills-soft>
                <xsl:for-each-group select="//skills[@class='soft']/descendant::skill" group-by="id">
                    <xsl:sort select="id"/>
                    <xsl:apply-templates select="current-group()[1]"><xsl:with-param name="pre" select="'http://pes.cz/abstr/concept/competence/skill/skill-soft/'" tunnel="yes"/></xsl:apply-templates>
                </xsl:for-each-group>
            </skills-soft>
            <skills-general>
                <xsl:for-each-group select="//skills[@class='general']/descendant::skill" group-by="id">
                    <xsl:sort select="id"/>
                    <xsl:apply-templates select="current-group()[1]"><xsl:with-param name="pre" select="'http://pes.cz/abstr/concept/competence/skill/skill-general/'" tunnel="yes"/></xsl:apply-templates>
                </xsl:for-each-group>
            </skills-general>
        </competences>
    </xsl:template>
    
    <xsl:template match="level"/>
    
    <xsl:template match="id">
        <xsl:param name="pre" tunnel="yes"/>
        <xsl:copy>
           <xsl:value-of select="concat($pre,.)"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>