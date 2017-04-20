package it.uniroma2.art.semanticturkey.rbac;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;

public class RBACProcessor {

	public static String tboxTheoryLocation = "/it/uniroma2/art/semanticturkey/rbac/rbac_tbox.pl";
	Prolog engine;
	String role;
	File roleFile;

	public RBACProcessor(File roleFile) throws InvalidTheoryException, TheoryNotFoundException {
		this.roleFile = roleFile;
		engine = new Prolog();
		String fileName = roleFile.getName();
		role = fileName.substring(fileName.indexOf("role_") + 5, fileName.indexOf(".pl"));
		initializeResources(roleFile);
	}
	
	public RBACProcessor(String role) throws InvalidTheoryException, TheoryNotFoundException {
		this.role = role;
		this.roleFile = RBACManager.getRoleFile(null, role);
		engine = new Prolog();
		initializeResources(roleFile);
	}

	public void initializeResources(File roleFile) throws InvalidTheoryException, TheoryNotFoundException {
		// creating initial theory (consulting a file)
		try {
			engine.setTheory(new Theory(RBACProcessor.class.getClassLoader().getResourceAsStream(tboxTheoryLocation)));
		} catch (IOException e) {
			throw new TheoryNotFoundException("a problem occuring in loading the RBAC TBOX occurred", e);
		}
		// loading a new theory and adding it to the first one (adding a role to the core RBAC rules)
		Theory roleTheory;
		try (FileInputStream fis = new FileInputStream(roleFile)) {
			roleTheory = new Theory(fis);
		} catch (IOException e) {
			throw new TheoryNotFoundException("a problem occuring in loading the USER ROLE model occurred", e);
		}
		engine.addTheory(roleTheory);
	}

	private void resetEngine() throws InvalidTheoryException, TheoryNotFoundException {
		engine = new Prolog();
		initializeResources(roleFile);
	}
	
	public String getRole() {
		return this.role;
	}

	/**
	 * this method will return a Prolog list of the capabilities
	 * 
	 * @return
	 * @throws MalformedGoalException
	 * @throws NoSolutionException
	 */
	public Term getCapabilitiesAsListTerm() throws MalformedGoalException, NoSolutionException {
		SolveInfo info = engine.solve("getCapabilities(FACTLIST).");
		return info.getVarValue("FACTLIST");
	}

	/**
	 * this method will return a Java list of prolog Terms, and each of the terms is a capability entry;
	 * 
	 * @return
	 * @throws MalformedGoalException
	 * @throws NoSolutionException
	 * @throws NoMoreSolutionException
	 */
	public List<Term> getCapabilitiesAsTermList()
			throws MalformedGoalException, NoSolutionException, NoMoreSolutionException {
		return getSolutionsAsList("capability(CAP, CRUD).");
	}

	public void runInterpreter() throws Exception {

		// this is just a listener for letting the Prolog engine send writes to the console
		// I added it in order to better interact with the pl files' content
		// you can decomment it if you want to follow the resolution order

		// engine.addOutputListener(new OutputListener() {
		// public void onOutput(OutputEvent e) {
		// System.out.print(e.getMsg());
		// }
		// });

		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		while (true) { // interpreter main loop
			String goal;
			do {
				System.out.print("?- ");
				goal = stdin.readLine();
			} while (goal.equals(""));
			try {
				// long ms = System.currentTimeMillis();
				SolveInfo info = engine.solve(goal);
				if (engine.isHalted())
					break;
				else if (!info.isSuccess())
					System.out.println("no.");
				else if (!engine.hasOpenAlternatives()) {
					System.out.println(info);
				} else {
					// main case
					System.out.println(info + " ?");
					String answer = stdin.readLine();
					while (answer.equals(";") && engine.hasOpenAlternatives()) {
						info = engine.solveNext();
						if (!info.isSuccess()) {
							System.out.println("no.");
							break;
						} else {
							System.out.println(info + " ?");
							answer = stdin.readLine();
						}
						// endif
					}
					// endwhile
					if (answer.equals(";") && !engine.hasOpenAlternatives())
						System.out.println("no.");
				}
				// end main case
				// System.out.println(System.currentTimeMillis() - ms);
			} catch (MalformedGoalException ex) {
				System.err.println("syntax error.");
			}
			// end try
		}
		// end interpreter main loop

		/*
		 * dumping the full theory if (args.length > 1) { Theory curTh = engine.getTheory(); // save current
		 * theo)ry to file new FileOutputStream(args[1]).write(curTh.toString().getBytes()); }
		 */
	}

	/*
	 * SHORT MANUAL
	 * 
	 * when writing, it is possible to create facts through the Struct object
	 * 
	 * Struct clause1 = new Struct(":-", new Struct("p",new Var("X")), new Struct("q",new Var("X")));
	 * 
	 * and even to create lists of facts this way
	 * 
	 * Struct clauseList = new Struct(clause1, new Struct(clause2, new Struct()));
	 * 
	 * 
	 */

	public List<Term> getSolutionsAsList(String goal)
			throws MalformedGoalException, NoSolutionException, NoMoreSolutionException {
		SolveInfo preInfo = engine.solve(goal);
		ArrayList<Term> caps = new ArrayList<>();
		if (preInfo.isSuccess()) {
			caps.add(preInfo.getSolution());
			while (preInfo.hasOpenAlternatives()) {
				preInfo = engine.solveNext();
				if (preInfo.isSuccess())
					caps.add(preInfo.getSolution());
			}
		}
		return caps;
	}

	public boolean authorizes(String goal) throws MalformedGoalException, HaltedEngineException, HarmingGoalException {
		SolveInfo info = engine.solve(goal);
		if (engine.isHalted()) {
			try {
				resetEngine();
			} catch (InvalidTheoryException | TheoryNotFoundException e) {
				throw new HaltedEngineException(
						"the RBAC engine has been halted in checking the following authorization:\n" + goal
								+ "\nand it is not able to restart");
			}
			throw new HarmingGoalException (
					"the RBAC engine has been halted in checking the following authorization:\n" + goal
							+ "\nengine has been restarted");
		} else if (info.isSuccess())
			return true;
		return false;
	}

	public static void main(String args[]) throws Exception {
		String role = args[0];
		RBACProcessor rbac = new RBACProcessor(role);
		System.out.println("capability list term: " + rbac.getCapabilitiesAsListTerm());
		System.out.println("capability term list: " + rbac.getCapabilitiesAsTermList());

		if (args.length > 1)
			try {
				System.out.println("checking authorization for: " + args[1] + " outcome: " + rbac.authorizes(args[1]));
			} catch (HarmingGoalException e) {
				System.err.println(e.getMessage());
			}
		
		if (args.length > 1)
			System.out.println("checking solutions for authorizing: " + args[1] + " outcome: " + rbac.getSolutionsAsList(args[1]));

		rbac.runInterpreter();
	}

}