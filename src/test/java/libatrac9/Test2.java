/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libatrac9;

import org.junit.jupiter.api.Test;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-02-03 nsano initial version <br>
 */
public class Test2 {

    @Test
    void test1() throws Exception {
        byte[] at9 = Atrac9.loadAt9("src/test/resources/snd0.at9");
    }
}
