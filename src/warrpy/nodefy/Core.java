package warrpy.nodefy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;

import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUtils;

public class Core extends JComponent {
	
	FontRenderContext font_render_context;
	Node node, active;
	Timer update;
	
	Color root_color = new Color(255, 204, 0), curve_color = new Color(112, 255, 42),
			ground_color = new Color(56, 56, 56), bulb_color = new Color(255, 20, 20),
			text_color = new Color(26, 26, 26), node_color = new Color(245, 255, 0);
	String save_to;
	
	AffineTransform transform = new AffineTransform();
	BasicStroke stroke = new BasicStroke(1.6f);
	Font font = new Font("Sans Serif", 0, 23);
	
	boolean short_update, update_graphics, wheel, edit_mode, svg_export, drag;
	final float X_OFFSET = 14, Y_OFFSET = 28.8f;
	float scale = 1, bulb_size;
	int frames, fps, fps2, timeout;
	

	/**
	 * <h1>Rendering engine</h1>
	 */
	public Core() {
		update = new Timer(16, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!wheel && !edit_mode) {
					update_graphics = false;
					wheel = true;
				}
				if (!update_graphics) {
					frames++;
					if (frames == 90) {
						// Stops update timer after 90s if update_graphics is false (Optimization).
						frames = 0;
						update.stop();
					}
				}
				repaint();
				fps++;
				wheel = false;
			}
		});

		var fps_counter = new Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fps2 = fps;
				fps = 0;
				//System.out.println(fps2); // Prints FPS (Debug).
			}
		});
		update.start();
		fps_counter.start();
	}
	
	/**
	 * <h1>Called by every repaint</h1>
	 */
	public void paintComponent(Graphics g) {
		if (svg_export) {
			SVGGraphics2D g2d = new SVGGraphics2D(getWidth(), getHeight());
			render(g2d);
			write_svg(save_to, g2d);
			svg_export = false;
			update_graphics = true;
		} else {
			Graphics2D g2d = (Graphics2D) g;
			render(g2d);
		}
	}
	
	public void render(Graphics2D g2d) {
		g2d.setColor(ground_color);
		g2d.fillRect(0, 0, getWidth(), getHeight());
	    g2d.translate(transform.getTranslateX(), transform.getTranslateY());
		g2d.scale(scale, scale);
		g2d.fill(new Rectangle2D.Float(0, 0, 20, 20));
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setFont(font);
		g2d.setStroke(stroke);
		font_render_context = g2d.getFontRenderContext();
		TextLayout layout = new TextLayout(node.text, font, font_render_context);
		node.width = (float) layout.getBounds().getWidth() + 30;
		// Draws link curves for all child nodes of the root node.
		g2d.setColor(curve_color);
		for (int i = 0; i < node.child_nodes.size(); i++) {
			float dist = (node.child_nodes.get(i).position_x - (node.position_x + node.width)) / 2;
			g2d.draw(new CubicCurve2D.Float(node.position_x + node.width, node.position_y + 20,
					node.position_x + node.width + dist, node.position_y + 20,
					node.child_nodes.get(i).position_x - dist, node.child_nodes.get(i).position_y + 20,
					node.child_nodes.get(i).position_x, node.child_nodes.get(i).position_y + 20));
		}
		// Draws root node.
		g2d.setColor(root_color);
		g2d.fill(new RoundRectangle2D.Float(node.position_x, node.position_y, node.width, node.height, 40, 40));
		g2d.setColor(bulb_color);
		bulb_animator(node, g2d);
		g2d.setColor(text_color);
		g2d.drawString(node.text, node.position_x + X_OFFSET, node.position_y + Y_OFFSET);
		// Draws all child nodes of the root node.
		tree_render(g2d, node);
		//
		getToolkit().sync(); // Rendering are platform-specific, this line makes the graphics smoother on linux.
		//g2d.dispose();
	}
	
	/**
	 * <h1>A recursive method that draws a tree of nodes</h1>
	 * Draws all child nodes of parameter node.
	 * @param g2d
	 * @param node
	 */
	public void tree_render(Graphics2D g2d, Node node) {
		for (Node block : node.child_nodes) {
			if (!block.child_nodes.isEmpty()) {
				for (int i = 0; i < block.child_nodes.size(); i++) {
					float x = block.child_nodes.get(i).position_x;
					float y = block.child_nodes.get(i).position_y;
					float dist = (x - (block.position_x + block.width)) / 2;
					g2d.setColor(curve_color);
					g2d.draw(new CubicCurve2D.Float(block.position_x + block.width, block.position_y + 20,
							block.position_x + block.width + dist, block.position_y + 20, x - dist, y + 20, x, y + 20));
				}
			}

			g2d.setColor(block.color);
			g2d.fill(new RoundRectangle2D.Float(block.position_x, block.position_y, block.width, block.height, 40, 40));
			bulb_animator(block, g2d);
			g2d.setColor(text_color);
			TextLayout layout = new TextLayout(block.text, font, font_render_context);
			block.width = (float) layout.getBounds().getWidth() + 30;
			g2d.drawString(block.text, block.position_x + X_OFFSET, block.position_y + Y_OFFSET);
			//
			tree_render(g2d, block); // A recursive drawing process. 
		}
	}

	public void draw_bulb(Graphics2D g2d, Node block) {
		g2d.setColor(ground_color);
		float bias_x = block.position_x + 13;
		float bias_y = block.position_y + 44;
		g2d.fill(new Ellipse2D.Float(bias_x - (bulb_size + 7) / 2, bias_y - (bulb_size + 7) / 2, bulb_size + 7,
				bulb_size + 7));
		g2d.setColor(bulb_color);
		g2d.fill(new Ellipse2D.Float(bias_x - bulb_size / 2, bias_y - bulb_size / 2, bulb_size, bulb_size));
	}

	public void bulb_animator(Node block, Graphics2D g2d) {
		if (block.edit_mode) {
			active = block;
			draw_bulb(g2d, block);
			if (bulb_size < 17) {
				bulb_size += 2;
			}
		} else {
			if (block.equals(active)) {
				draw_bulb(g2d, block);
				if (bulb_size >= 0) {
					bulb_size -= 3;
				} else {
					active = null;
				}
			}
		}
	}

	public void export_image(String path) {
		BufferedImage bImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bImg.createGraphics();
		paintAll(g2d);
		try {
			if (ImageIO.write(bImg, "png", new File(path))) {
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void export_svg(String path) {
		svg_export = true;
		save_to = path;
	}

	public void write_svg(String path, SVGGraphics2D g2d) {

		try {
			SVGUtils.writeToSVG(new File(path), g2d.getSVGElement());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class Node {
	ArrayList<Node> child_nodes = new ArrayList<Node>();
	Color color = new Color(245, 255, 0);
	String text = "Mindmap";
	float position_x, position_y, width = 130f, height = 40f;
	boolean edit_mode;
	
	/**
	 * <h1>Node object</h1>
	 * @param position_x
	 * @param position_y
	 */
	public Node(float position_x, float position_y) {
		this.position_x = position_x;
		this.position_y = position_y;
	}
}
