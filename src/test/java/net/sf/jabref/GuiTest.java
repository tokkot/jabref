package net.sf.jabref;

import org.junit.Test;

public class GuiTest {

    @Test
    public void testStartAndStop(){
        JabRefMain.main(new String[]{});
        JabRef.jrf.quit();
    }

}
