package com.rotacerta.api.config;

import com.rotacerta.api.model.entities.Plan;
import com.rotacerta.api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

	private final PlanRepository planRepository;
	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	public static final String FREE_PLAN_NAME = "Plano Gratuito";
	public static final String PREMIUM_PLAN_NAME = "Plano Premium";

	@Override
	public void run(String... args) throws Exception {
		if (planRepository.count() == 0) {
			log.info("Nenhum plano encontrado. Criando planos padrão Freemium...");

			Plan freePlan = new Plan();
			freePlan.setName(FREE_PLAN_NAME);
			freePlan.setPrice(0.0);
			freePlan.setQuoteLimit(10);
			freePlan.setStripePriceId("free_plan_01");

			Plan premiumPlan = new Plan();
			premiumPlan.setName(PREMIUM_PLAN_NAME);
			premiumPlan.setPrice(29.90);
			premiumPlan.setQuoteLimit(5000);
			premiumPlan.setStripePriceId("price_1S4RqJPH4tTzeMs7WYAmb6dt");

			planRepository.saveAll(List.of(freePlan, premiumPlan));

			log.info("Planos Freemium criados com sucesso!");
		} else {
			log.info("Planos já existem no banco de dados.");
		}
	}
}

