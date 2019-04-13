/**
 * @author Prasanna Parthasarathy
 *
 */
public class Main {
	
	public static void main(String args[]) {
		if(args.length < 1) {
			System.out.println("Insufficient command arguments.");
			return;
		}
		if(args[0] != null && !args[0].isEmpty()) {
			if(args[0].equalsIgnoreCase("server")) {
				try {
					int port = 8000;
					if(args[2] != null && !args[2].isEmpty())
						port = Integer.parseInt(args[2]);
					new FileServer(port, args[1]);
				} catch(ArrayIndexOutOfBoundsException a) {
					System.err.println("Insufficient arguments.");
				}
				
			} else if(args[0].equalsIgnoreCase("client")) {
				new FSSClient(args);
			} else {
				System.out.println("Invalid arguments.");
			}
		}
	}

}
