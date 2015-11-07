package sorcer.sml.arithmetic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.response;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class ArithmeticMograms {
	private final static Logger logger = LoggerFactory.getLogger(ArithmeticMograms.class);

	@Test
	public void lambdaModel() throws Exception {

		Model mo = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
				ent("add/x1", 20.0), ent("add/x2", 80.0),
				ent("add", model ->
						(double) value(model, "add/x1") + (double) value(model, "add/x2")),
				ent("multiply", model ->
						(double) val(model, "multiply/x1") * (double) val(model, "multiply/x2")),
				ent("subtract", model ->
						(double) v(model, "multiply") - (double) v(model, "add")),
				response("subtract", "multiply", "add"));

		dependsOn(mo, ent("subtract", paths("multiply", "add")));

		Context out = response(mo);
		logger.info("model response: " + out);
		assertTrue(get(out, "subtract").equals(400.0));
		assertTrue(get(out, "multiply").equals(500.0));
		assertTrue(get(out, "add").equals(100.0));
	}

	@Test
	public void lambdaModelWithReturnPath() throws Exception {

		Model mo = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
				ent("add/x1", 20.0), ent("add/x2", 80.0),
				ent("arg/x1", 30.0), ent("arg/x2", 90.0),
				ent("add", model ->
						(double) value(model, "add/x1") + (double) value(model, "add/x2"),
						result("add/out",
								inPaths("add/x1", "add/x2"))),
				ent("multiply", model ->
						(double) val(model, "multiply/x1") * (double) val(model, "multiply/x2"),
						result("multiply/out",
								inPaths("multiply/x1", "multiply/x2"))),
				ent("subtract", model ->
						(double) v(model, "multiply/out") - (double) v(model, "add/out"),
						result("model/response")),
				response("subtract", "multiply/out", "add/out", "model/response"));

		dependsOn(mo, ent("subtract", paths("multiply", "add")));

		Context out = response(mo);
		logger.info("model response: " + out);
		assertTrue(get(out, "model/response").equals(400.0));
		assertTrue(get(out, "multiply/out").equals(500.0));
		assertTrue(get(out, "add/out").equals(100.0));
	}

	@Test
	public void sigLocalModel() throws Exception {
		// get responses from a local service model

		Model m = model(
				inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
				inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
				ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				ent(sig("add", AdderImpl.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				ent(sig("subtract", SubtractorImpl.class, result("model/response",
						inPaths("multiply/out", "add/out")))),
				response("subtract"));

		dependsOn(m, ent("subtract", paths("multiply", "add")));
//        logger.info("response: " + response(m));
		Context out = response(m);

		assertTrue(get(out, "subtract").equals(400.0));
	}

	@Test
	public void sigRemoteModel() throws Exception {
		// get responses from a remote service model

		Model m = model(
				inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
				inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
				ent(sig("multiply", Multiplier.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				ent(sig("add", Adder.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				ent(sig("subtract", Subtractor.class, result("model/response",
						inPaths("multiply/out", "add/out")))),
				response("subtract"));

		dependsOn(m, ent("subtract", paths("multiply", "add")));
//        logger.info("response: " + response(m));
		Context out = response(m);

		assertTrue(get(out, "subtract").equals(400.0));
	}

	@Test
	public void sigMixedModel() throws Exception {
		// get responses from a remote service model

		Model m = model(
				inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
				inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
				ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				ent(sig("add", AdderImpl.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				ent(sig("subtract", Subtractor.class, result("model/response",
						inPaths("multiply/out", "add/out")))),
				response("subtract"));

		dependsOn(m, ent("subtract", paths("multiply", "add")));
//        logger.info("response: " + response(m));
		Context out = response(m);

		assertTrue(get(out, "subtract").equals(400.0));
	}

    @Test
    public void batchTask() throws Exception {
        // batch for the composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
        // shared context with named paths
        Task batch3 = task("batch3",
                type(sig("multiply", MultiplierImpl.class,
						result("subtract/x1", Signature.Direction.IN)), Signature.PRE),
                type(sig("add", AdderImpl.class,
						result("subtract/x2", Signature.Direction.IN)), Signature.PRE),
                sig("subtract", SubtractorImpl.class,
						result("result/y", inPaths("subtract/x1", "subtract/x2"))),
                context(inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                        inEnt("add/x1", 20.0), inEnt("add/x2", 80.0)));

        logger.info("task getSelects:" + batch3.getFidelity());

        batch3 = exert(batch3);
		//logger.info("task result/y: " + get(batch3, "result/y"));
		assertEquals(get(batch3, "result/y"), 400.0);
    }

	@Test
	public void arithmeticBlock() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Signature.Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
						result("arg/t4", Signature.Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x3"), inEnt("arg/x4"),
						result("arg/t5", Signature.Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
				inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
				inEnt("arg/x3", 20.0), inEnt("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);
	}

	@Test
	public void jobPipeline() throws Exception {

		Task t3 = task("t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
						outEnt("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Task t5 = task("t5",
				sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		Job job = job(sig("service", ServiceJobber.class),
				"j1", t4, t5, t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job context: " + context);
		assertTrue(get(context, "j1/t3/result/y").equals(400.0));
	}

	@Test
	public void nestedJob() throws Exception {

		Task t3 = task("t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
						outEnt("result/y", null)));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Task t5 = task("t5",
				sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job context: " + context);
		assertTrue(get(context, "j1/t3/result/y").equals(400.0));
	}

}
	
	
