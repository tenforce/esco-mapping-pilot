<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:skos="http://www.w3.org/2004/02/skos/core#"
    xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    exclude-result-prefixes="#all"
    version="2.0">
    <xd:doc scope="stylesheet">
        <xd:desc>
            <xd:p>Transform CZECH skill x,l to RDF for ESCO</xd:p>
            <xd:author>Horst Kucharczyk</xd:author>
            <xd:history>
                <xd:change version="0.01" date="2015-08-24">
                    <xd:desc>
                        <xd:p>creation</xd:p>
                        <xd:pre>
                            java -jar C:\tools\SaxonHE9-6-0-7J\saxon9he.jar exportPozic-HOSP.xml -o:exportHOSP-norm.xml -xsl:normalise-src.xsl
                            java -jar C:\tools\SaxonHE9-6-0-7J\saxon9he.jar exportHOSP-norm.xml -o:competencesHOSP.xml -xsl:collect-skills.xsl                            
                            java -jar C:\tools\SaxonHE9-6-0-7J\saxon9he.jar competencesHOSP.xml -o:cz-competenceHOSP.ttl -xsl:skill-xml2ttl.xsl
                            java -jar C:\tools\SaxonHE9-6-0-7J\saxon9he.jar exportHOSP-norm.xml -o:cz-occupanciesHOSP.ttl -xsl:occupancy-xml2ttl.xsl
                        </xd:pre>
                    </xd:desc>
                    <xd:contributor>Horst Kucharczyk</xd:contributor>
                </xd:change>
                <xd:change version="0.02" date="2015-08-28">
                    <xd:desc>
                        <xd:p>tuned for occupancies</xd:p>
                    </xd:desc>
                    <xd:contributor>Horst Kucharczyk</xd:contributor>
                </xd:change>
            </xd:history>
        </xd:desc>
    </xd:doc>
    
    <xsl:output encoding="UTF-8" method="xml" indent="yes"/>    
    
    <xsl:template match="/">
        <xsl:apply-templates/>        
    </xsl:template>
       
    <xsl:template match="*">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="@*">
        <xsl:attribute name="{local-name()}" select="."/>
    </xsl:template>
    
    <xsl:template match="text()">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:template>
    
    <xsl:key name="occupancy-prefLabel" match="//Identifikace" use="NazevJP"/>
    <xsl:key name="occupancy-alternate-route" match="//PodrizenaTP" use="NazevPodrizenaTP"/>
    
    <!-- high level -->
    <xd:doc scope="component">
        <xd:desc>omit from the result</xd:desc>
    </xd:doc>
    <xsl:template match="Cinnosti|Garance|Isco4MzdyVRegionech|KvalUroven|Nutnost|PrPraciPodnik|PrPraciRozpoct|PracPodminky|PripravaAVzdelani|ZdravPodminky"/>
    
    <xd:doc scope="component">
        <xd:desc>process through</xd:desc>
    </xd:doc>
    <xsl:template match="OdborneZn"><xsl:apply-templates/></xsl:template>
    
    <xd:doc scope="component">
        <xd:desc>de facto root</xd:desc>
    </xd:doc>
    <xsl:template match="JednotkyPrace">
        <occupancies>
            <xsl:namespace name="skos" select="'http://www.w3.org/2004/02/skos/core#'"/>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </occupancies>        
        
    </xsl:template>
    
    <xsl:template match="JednotkaPrace">
        <occupancy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </occupancy>
    </xsl:template>
    
    <xsl:template match="AltNazvy">
        <altLabels>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </altLabels>
    </xsl:template>
    
    <xsl:template match="AltNazev">
        <skos:altLabel>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </skos:altLabel>
    </xsl:template>
    
    <xsl:template match="NazevJP|NazevMKN|NazevOZn|OdborneDovNazev|Nazev|NazevPodrizenaTP">
        <!-- context is clue -->
        <skos:prefLabel>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </skos:prefLabel>
    </xsl:template>
    
    <xsl:template match="Charakteristika">
        <skos:definition xml:lang="cs">
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </skos:definition>
    </xsl:template>
    
    <xsl:template match="Identifikace">
        <indentity>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </indentity>
    </xsl:template>
    
    <xsl:template match="OdborneSmery">
        <professional-areas>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </professional-areas>
    </xsl:template>
    
    <xsl:template match="OdbornaZnalost">
        <knowledges class="professional">
            <xsl:apply-templates select="@*"/>
            <xsl:choose>
                <xsl:when test="Nutnost='Nutné'">
                    <essential>
                        <xsl:apply-templates select="node()"/>
                    </essential>
                </xsl:when>
                <xsl:when test="Nutnost='Výhodné'">
                    <optional>
                        <xsl:apply-templates select="node()"/>
                    </optional>
                </xsl:when>
            </xsl:choose> 
        </knowledges>
    </xsl:template>
    
    <xsl:template match="OdborneDov">
        <skills class="professional">
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </skills>
    </xsl:template>
    
    <xsl:template match="Nutne">
        <essential>
            <xsl:apply-templates select="node()"/>
        </essential>
    </xsl:template>
    
    <xsl:template match="Vyhodne">
        <optional>
            <xsl:apply-templates select="node()"/>
        </optional>
    </xsl:template>
    
    <xsl:template match="PodrizeneTP">
        <narrowerTerms>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </narrowerTerms>
    </xsl:template>
    
    <xsl:template match="PodrizenaTP">
        <narrowerTerm>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </narrowerTerm>
    </xsl:template>
    
    <xsl:template match="MekkeKomp">
        <skills class="soft">
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </skills>
    </xsl:template>
    
    <xsl:template match="ObecneDovednosti">
        <skills class="general">
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </skills>
    </xsl:template>
    
    <!-- details -->
    
    <xsl:template match="PolozkaOD|OdborneDovPolozka|PolozkaMKN">
        <skill>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </skill>
    </xsl:template>
        
    <xsl:template match="Polozka">
        <knowledge>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </knowledge>
    </xsl:template>
    
    <xsl:template match="Polozky">
        <xsl:apply-templates select="node()"/>
    </xsl:template>
    
    <xsl:template match="Uroven">
        <level><xsl:value-of select="normalize-space(substring-after(.,'Úroveň '))"/></level>
    </xsl:template>
    
    <xsl:template match="OdborneDovUroven|UrovenOZn">
        <level><xsl:value-of select="normalize-space(.)"/></level>
    </xsl:template>
    
    <xsl:template match="OdborneDovKod|KodMKN|KodOZn|KodJP|Kod|KodPodrizenaTP">
        <id><xsl:value-of select="normalize-space(.)"/></id>
    </xsl:template>
    
    <xsl:template match="PribuzneTP">
        <relatedTerms>
            <xsl:apply-templates/>
        </relatedTerms>
    </xsl:template>
    
    <xsl:template match="NazevPribTP">     
        <relatedTerm>
            <xsl:choose>
                <xsl:when test="key('occupancy-prefLabel',.)/KodJP">
                    <xsl:attribute name="id" select="key('occupancy-prefLabel',.)[1]/KodJP"/>
                </xsl:when>
                <xsl:when test="key('occupancy-alternate-route',.)/KodPodrizenaTP">
                    <xsl:attribute name="id" select="key('occupancy-alternate-route',.)[1]/KodPodrizenaTP"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="id" select="FINDFF"/>
                </xsl:otherwise>
            </xsl:choose>            
            <xsl:apply-templates/>
        </relatedTerm>
    </xsl:template>
    
    <xsl:template match="NadrizenaJP">
        <broaderTerms>
            <xsl:apply-templates/>
        </broaderTerms>
    </xsl:template>
    
    <xsl:template match="NazevNadrJP">
        <broaderTerm>
            <xsl:attribute name="id">
                <xsl:value-of select="key('occupancy-prefLabel',.)[1]/KodJP"/>
            </xsl:attribute>
            <xsl:apply-templates/>
        </broaderTerm>
    </xsl:template>
    
    <xsl:template match="TypJP">
        <OccupationType>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
        </OccupationType>
    </xsl:template>
</xsl:stylesheet>