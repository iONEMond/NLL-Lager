package de.nll.lager;

import org.junit.Test;
import static org.junit.Assert.*;

public class RulesTest {
    @Test public void exactDecimalQuantities() {
        assertEquals("0.125",Rules.quantity("0,125"));
        assertEquals("12.5",Rules.quantity(" 012.500 "));
        assertEquals("999999999.999",Rules.quantity("999999999.999"));
    }
    @Test public void invalidAmountsAreRejected() {
        for(String value:new String[]{"0","-1","1e3","NaN","0,0001","1.000,5","1000000000","", ".5"}) {
            assertThrows(value,IllegalArgumentException.class,() -> Rules.quantity(value));
        }
    }
    @Test public void destinationsMustDiffer() {
        assertThrows(IllegalArgumentException.class,() -> Rules.locations(" Lager A ","lager a"));
        assertThrows(IllegalArgumentException.class,() -> Rules.locations("", "B"));
        Rules.locations("Lager A","Baustelle 17");
    }
    @Test public void codesPreserveLeadingZerosAndRejectControlCharacters() {
        assertEquals("000123",Rules.required("000123","Code"));
        assertThrows(IllegalArgumentException.class,() -> Rules.required("123\n456","Code"));
    }
    @Test public void csvPreservesQuotedValuesAndProtectsSpreadsheetFormulas() {
        assertEquals("\"Text; mit \"\"Zitat\"\"\"",Rules.csv("Text; mit \"Zitat\""));
        assertEquals("\"'=1+1\"",Rules.csv("=1+1"));
        assertEquals("\"'  @SUM(A1)\"",Rules.csv("  @SUM(A1)"));
        assertEquals("\"'\t=cmd\"",Rules.csv("\t=cmd"));
        assertEquals("\"000123\"",Rules.csv("000123"));
        assertEquals("\"\"",Rules.csv(null));
    }
}
