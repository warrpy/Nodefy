package warrpy.nodefy;

import java.awt.EventQueue;

	/**
	 * <h1>Nodefy - a simple program for creating mind maps</h1>
	 * ---------------------------------------------------------
	 * @category Productivity
	 * @apiNote Used External libs: Radcode and JFreeSVG.
	 * @author Warrpy
	 * @version 1.0
	 */

public class Nodefy {

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Interface frame = new Interface();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}