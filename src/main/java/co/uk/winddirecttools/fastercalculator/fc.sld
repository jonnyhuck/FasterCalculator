<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Ten color gradient</Name>
    <UserStyle>
      <Title>ColorBrewer Top 10 by Jonny Huck</Title>
      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap>
		<ColorMapEntry color="#FFFFFF" quantity="0" opacity="0"/>
	      	<ColorMapEntry color="#8DD3C7" quantity="1"/>
              	<ColorMapEntry color="#FFFFB3" quantity="2"/>
		<ColorMapEntry color="#FB8072" quantity="3"/>
		<ColorMapEntry color="#80B1D3" quantity="4"/>
		<ColorMapEntry color="#FDB462" quantity="5"/>
		<ColorMapEntry color="#B3DE69" quantity="6"/>
		<ColorMapEntry color="#FCCDE5" quantity="7"/>
		<ColorMapEntry color="#D9D9D9" quantity="8"/>
		<ColorMapEntry color="#BC80BD" quantity="9"/>
		<ColorMapEntry color="#CCEBC5" quantity="10"/>
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>