package com.mtg.mtgwalletbe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("unittest") // offline: H2, see src/test/resources/application-unittest.properties
class MtgWalletBeApplicationTests {

	@Test
	void contextLoads() {
	}

}
