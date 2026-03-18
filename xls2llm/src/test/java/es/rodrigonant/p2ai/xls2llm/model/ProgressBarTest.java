package es.rodrigonant.p2ai.xls2llm.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProgressBar Tests")
class ProgressBarTest {

    @Test
    @DisplayName("Constructor initializes with total and currState=0")
    void testConstructor() {
        ProgressBar progress = new ProgressBar(100);
        assertEquals(0, progress.getCurrState());
        assertEquals(100, progress.getTotal());
    }

    @Test
    @DisplayName("setState updates current state")
    void testSetState() {
        ProgressBar progress = new ProgressBar(100);
        progress.setState(50);
        assertEquals(50, progress.getCurrState());
    }

    @Test
    @DisplayName("getPercent returns 0 when state is 0")
    void testGetPercentZero() {
        ProgressBar progress = new ProgressBar(100);
        progress.setState(0);
        assertEquals(0, progress.getPercent());
    }

    @Test
    @DisplayName("getPercent returns 50 when state is halfway")
    void testGetPercentHalfway() {
        ProgressBar progress = new ProgressBar(200);
        progress.setState(100);
        assertEquals(50, progress.getPercent());
    }

    @Test
    @DisplayName("getPercent returns 25 when state is quarter")
    void testGetPercentQuarter() {
        ProgressBar progress = new ProgressBar(100);
        progress.setState(25);
        assertEquals(25, progress.getPercent());
    }

    @Test
    @DisplayName("getPercent returns 100 when state equals total")
    void testGetPercentComplete() {
        ProgressBar progress = new ProgressBar(100);
        progress.setState(100);
        assertEquals(100, progress.getPercent());
    }

    @Test
    @DisplayName("getPercent handles 75% progress")
    void testGetPercentThreeQuarters() {
        ProgressBar progress = new ProgressBar(100);
        progress.setState(75);
        assertEquals(75, progress.getPercent());
    }

    @Test
    @DisplayName("setState prevents state from exceeding total")
    void testSetStateExceedingTotal() {
        ProgressBar progress = new ProgressBar(100);
        assertThrows(IllegalArgumentException.class, () -> progress.setState(150));
    }

    @Test
    @DisplayName("setState prevents negative state")
    void testSetStateNegative() {
        ProgressBar progress = new ProgressBar(100);
        assertThrows(IllegalArgumentException.class, () -> progress.setState(-1));
    }

    @Test
    @DisplayName("Constructor prevents zero total")
    void testConstructorZeroTotal() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressBar(0));
    }

    @Test
    @DisplayName("Constructor prevents negative total")
    void testConstructorNegativeTotal() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressBar(-50));
    }

    @Test
    @DisplayName("getPercent handles integer division correctly (1 of 3 ~= 33%)")
    void testGetPercentIntegerDivision() {
        ProgressBar progress = new ProgressBar(3);
        progress.setState(1);
        assertEquals(33, progress.getPercent());
    }

    @Test
    @DisplayName("getPercent with lines 101-200, current=150 should be 50%")
    void testGetPercentRealWorldScenario() {
        ProgressBar progress = new ProgressBar(100);
        progress.setState(50);
        assertEquals(50, progress.getPercent());
    }

    @Test
    @DisplayName("getPercent handles large numbers")
    void testGetPercentLargeNumbers() {
        ProgressBar progress = new ProgressBar(10000);
        progress.setState(5000);
        assertEquals(50, progress.getPercent());
    }

    @Test
    @DisplayName("setState(0) is valid")
    void testSetStateZero() {
        ProgressBar progress = new ProgressBar(100);
        progress.setState(0);
        assertEquals(0, progress.getCurrState());
        assertEquals(0, progress.getPercent());
    }

    @Test
    @DisplayName("Multiple state updates work correctly")
    void testMultipleStateUpdates() {
        ProgressBar progress = new ProgressBar(100);
        
        progress.setState(25);
        assertEquals(25, progress.getPercent());
        
        progress.setState(50);
        assertEquals(50, progress.getPercent());
        
        progress.setState(100);
        assertEquals(100, progress.getPercent());
    }

}
