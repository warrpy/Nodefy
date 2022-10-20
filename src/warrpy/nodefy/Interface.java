package warrpy.nodefy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import warrpy.radcode.Radcode;

public class Interface extends JFrame {
	private static final long serialVersionUID = 1L;

	JMenuBar menu_bar = new JMenuBar();
	JMenu project_menu = new JMenu(" Project ");
	JMenuItem svg_ex = new JMenuItem(" Export to SVG ");
	JMenuItem png_ex = new JMenuItem(" Export to image");
	JMenuItem save_project = new JMenuItem(" Save As");
	JMenuItem open_project = new JMenuItem(" Open Project ");
	JMenu export = new JMenu(" Export ");
	JMenuItem exit = new JMenuItem(" Exit ");
	JMenu theme = new JMenu(" Theme ");
	JMenu look = new JMenu(" Apperance ");
	JMenuItem default_theme = new JMenuItem("Default Theme");
	JMenuItem load_theme = new JMenuItem(" Load... ");
	JRadioButtonMenuItem def, metal, nimbus, gtk;
	
	boolean edit_mode, block_selected, clear_text, drag, autosave;
	float mouse_x, mouse_y, offset_x, offset_y, drag_value_x, drag_value_y, drag_offset_x, drag_offset_y;
	
	String tree_data = "", home = null, current_theme = "default", workspace, themeloc, app_theme = "0", text = "";

	UIThemeManager uitheme_manager = new UIThemeManager();
	Node selected_node, parent_node;
	Core core;
	
	/**
	 * <h1>Application window and logic</h1>
	 */
	public Interface() {
		create_window();
		add_listeners();
		load_properties();
	}
	/**
	 * <h1>Creates application window, contains menu item listeners.</h1>
	 */
	public void create_window() {
		setSize(1000, 550);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setLocationRelativeTo(null);
		setTitle("Nodefy");
		
		project_menu.add(open_project);
		project_menu.add(save_project);
		export.add(png_ex);
		export.add(svg_ex);
		project_menu.add(export);

		menu_bar.add(project_menu);
		setJMenuBar(menu_bar);

		// LookAndFeel
		def = new JRadioButtonMenuItem(" Default ");
		metal = new JRadioButtonMenuItem(" Metal ");
		nimbus = new JRadioButtonMenuItem(" Nimbus ");
		gtk = new JRadioButtonMenuItem(" GTK+ ");
		
		look.add(def);
		look.add(metal);
		look.add(nimbus);
		look.add(gtk);
		var look_group = new ButtonGroup();
		look_group.add(def);
		look_group.add(metal);
		look_group.add(nimbus);
		look_group.add(gtk);
		def.setSelected(true);
		project_menu.add(look);
		theme.add(default_theme);
		theme.add(load_theme);
		project_menu.add(theme);
		project_menu.add(exit);

		/*
		 * Core initialization, creating and adding root node.
		 */
		core = new Core();
		core.node = new Node(100, 100);
		add(core);
		/*
		 * 
		 */

		// Menu items listeners initialization.
		save_project.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save_project();
			}
		});

		open_project.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				load_project();
			}
		});

		png_ex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				core.export_image(file_manager(FileDialog.SAVE, "Export PNG"));
			}
		});

		svg_ex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				core.export_svg(file_manager(FileDialog.SAVE, "Export SVG"));
			}
		});

		default_theme.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				set_default_theme();
				current_theme = "default";
			}
		});

		load_theme.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				load_theme("OpenFile");
			}
		});

		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		def.addActionListener((event) -> {
			try {
				uitheme_manager.set_theme(0, this);
				app_theme = "0";
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		metal.addActionListener((event) -> {
			try {
				uitheme_manager.set_theme(1, this);
				app_theme = "1";
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		nimbus.addActionListener((event) -> {
			try {
				uitheme_manager.set_theme(2, this);
				app_theme = "2";
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		gtk.addActionListener((event) -> {
			try {
				uitheme_manager.set_theme(3, this);
				app_theme = "3";
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// Saving program Look&Feel theme on close.
				var radcode = new Radcode();
				var path = radcode.utils.get_home() + "/.Nodefy.data";
				var data = radcode.lparser.parse_list(path)[0];
				var new_data = data + "\n" + app_theme + "\n";
				radcode.files.create_file(path, new_data);
				
				// The project can be saved on exit without opening the file dialog (not used in build).
				// Uncomment lines below to enable autosave.
				
				//if (autosave) {
				//	save_project();
				//}
			}
		});

	}

	public void add_listeners() {
		core.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Moving the node.
				if (e.getButton() == MouseEvent.BUTTON1) {
					update_mouse_position(e);
					if (mouse_on_block(core.node)) {
						selected_node = core.node;
						block_selected = true;
					} else {
						find_node(core.node);
					}

					if (block_selected) {
						offset_x = mouse_x - selected_node.position_x;
						offset_y = mouse_y - selected_node.position_y;
						move_block();
					}
				}
				// Remove node or drag the tree.
				if (e.getButton() == MouseEvent.BUTTON2) {
					update_mouse_position(e);
					find_node(core.node);
					if (block_selected && edit_mode) {
						remove_node();
						core.bulb_size = 0;
						update_graphics();
						block_selected = false;
					} else {
						block_selected = false;
						drag = true;
						drag_value_x = (float) core.transform.getTranslateX();
						drag_value_y = (float) core.transform.getTranslateY();
						drag_offset_x = e.getX() - drag_value_x;
						drag_offset_y = e.getY() - drag_value_y;
					}
				}
				// Adding a child to the node to be selected.
				if (e.getButton() == MouseEvent.BUTTON3) {
					update_mouse_position(e);
					if (mouse_on_block(core.node)) {
						selected_node = core.node;
						block_selected = true;
					} else {
						find_node(core.node);
					}
					if (block_selected) {
						update_mouse_position(e);
						selected_node.edit_mode = false;
						offset_x = mouse_x - selected_node.position_x;
						offset_y = mouse_y - selected_node.position_y;
						var new_node = new Node(mouse_x - offset_x, mouse_y - offset_y);
						new_node.color = core.node_color;
						add_node(new_node);
						update_graphics();
					}
				}
			}

			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (e.getClickCount() > 1) {
						update_mouse_position(e);
						if (mouse_on_block(core.node)) {
							selected_node = core.node;
							block_selected = true;
						} else {
							find_node(core.node);
						}
						if (block_selected) {
							edit_mode = true;
							selected_node.edit_mode = true;
							update_graphics();
							core.edit_mode = true;
						}
					}
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (!edit_mode) {
						core.update_graphics = false;
					}
					block_selected = false;
				}
				if (e.getButton() == MouseEvent.BUTTON2) {
					drag = false;
					core.drag = false;
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					core.update_graphics = false;
					block_selected = false;
				}
				if (e.getButton() == MouseEvent.BUTTON3) {
					block_selected = false;
					core.update_graphics = false;
				}
			}
		});

		core.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				core.scale += e.getWheelRotation() * 0.02;
				core.wheel = true;
				update_graphics();
			}
		});

		core.addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(MouseEvent e) {
				update_mouse_position(e);
				if (drag) {
					setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
					drag_value_x = (float) core.transform.getTranslateX();
					drag_value_y = (float) core.transform.getTranslateY();
					core.drag = true;
					core.transform.setToTranslation(e.getX() - drag_offset_x, e.getY() - drag_offset_y);
					update_graphics();
				}
				if (block_selected) {
					move_block();
					update_graphics();
				}
			}

			public void mouseMoved(MouseEvent e) {
				if (edit_mode) {
					update_mouse_position(e);
					if (!mouse_on_block(selected_node)) {
						edit_mode = false;
						core.edit_mode = false;
						selected_node.edit_mode = false;
						core.update_graphics = false;
						block_selected = false;
						update_graphics();
					}
				}
			}
		});

		addKeyListener(new KeyListener() {
			boolean backspace = false;

			public void keyPressed(KeyEvent e) {
				if (edit_mode) {
					if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
						backspace = true;
					}
					if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
						clear_text = true;
					}
				}
			}
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
					clear_text = false;
				}
			}
			
			public void keyTyped(KeyEvent e) {
				if (edit_mode) {
					char c = e.getKeyChar();
					text = selected_node.text;
						if (backspace) {
							StringBuilder sb = new StringBuilder(text);
							sb.deleteCharAt(text.length() - 1);
							text = sb.toString();
							backspace = false;
							
							if (clear_text) {
								text = " ";
								clear_text = false;
							}

						} else {
							if (text.equals(" ")) {
								text = "";
							}
							text += c;
						}
						if (text.isEmpty()) {
							text = " ";
						}
						selected_node.text = text;
					}
			}
		});
	}
	
	/**
	 * <h1>Loads worksapce path and program Look&Feel</h1>
	 * 
	 */

	public void load_properties() {
		var radcode = new Radcode();
		String file = radcode.utils.get_home() + "/.Nodefy.data";
		if (radcode.files.file_exists(file)) {
			var data = radcode.lparser.parse_list(file);
			home = data[0];
			uitheme_manager.set_theme(Integer.valueOf(data[1]), this);
			switch (data[1]) {
			case "0":
				def.setSelected(true);
				break;
			case "1":
				metal.setSelected(true);
				break;
			case "2":
				nimbus.setSelected(true);
				break;
			case "3":
				gtk.setSelected(true);
			}
			workspace = home;
		}
	}
	
	/**
	 * <h1>Sets theme (colors)</h1>
	 * Loading depends on parameter.
	 * @param option "OpenFile" or path to theme_name.xml file
	 */
	public void load_theme(String option) {
		String file = "";

		if (option.equals("OpenFile")) {
			file = file_manager(FileDialog.LOAD, "Load Theme");
			themeloc = file;
		} else {
			file = option;
		}
		if (file.equals("nullnull")) {
			return;
		}

		var color_data = preload_theme(file);
		core.root_color = new Color(color_data.get(0)[0], color_data.get(0)[1], color_data.get(0)[2]);
		var node_color = new Color(color_data.get(1)[0], color_data.get(1)[1], color_data.get(1)[2]);
		core.node_color = node_color;
		paint_nodes(core.node, node_color);
		core.curve_color = new Color(color_data.get(2)[0], color_data.get(2)[1], color_data.get(2)[2]);
		core.text_color = new Color(color_data.get(3)[0], color_data.get(3)[1], color_data.get(3)[2]);
		core.ground_color = new Color(color_data.get(4)[0], color_data.get(4)[1], color_data.get(4)[2]);
		core.bulb_color = new Color(color_data.get(5)[0], color_data.get(5)[1], color_data.get(5)[2]);

		var new_theme = new JCheckBoxMenuItem(current_theme);
		theme.removeAll();
		new_theme.setSelected(true);
		theme.add(new_theme);
		theme.add(load_theme);
		new_theme.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!new_theme.isSelected()) {
					set_default_theme();
					theme.removeAll();
					theme.add(default_theme);
					theme.add(load_theme);
				}
			}
		});
		update_graphics();
	}
	
	/**
	 * <h1>Recursive method that change color of nodes</h1>
	 * Changes color for all child nodes of parameter node.
	 * @param node
	 * @param color
	 */
	public void paint_nodes(Node node, Color color) {
		for (Node block : node.child_nodes) {
			block.color = color;
			paint_nodes(block, color);
		}
	}

	public void set_default_theme() {
		core.root_color = new Color(255, 204, 0);
		core.curve_color = new Color(112, 255, 42);
		core.ground_color = new Color(56, 56, 56);
		core.bulb_color = new Color(255, 20, 20);
		core.text_color = new Color(26, 26, 26);
		core.node_color =  new Color(245, 255, 0);
		paint_nodes(core.node, core.node_color);
		current_theme = "default";
		themeloc = " ";
		theme.removeAll();
		theme.add(default_theme);
		theme.add(load_theme);
		update_graphics();
	}
	
	/**
	 * <h1>Loads theme (color) data from a file</h1>
	 * XML Parser
	 * @param path
	 * @return array of color values array
	 */
	public ArrayList<Integer[]> preload_theme(String path) {
		var color_data = new ArrayList<Integer[]>();
		try {
			File inputFile = new File(path);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			String properties[] = { "root", "node", "curve", "text", "ground", "bulb" };
			NodeList nList = doc.getElementsByTagName("theme");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				org.w3c.dom.Node nNode = nList.item(temp);
				if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					org.w3c.dom.Node name = eElement.getElementsByTagName("name").item(0);
					current_theme = name.getTextContent();
					for (String property : properties) {
						org.w3c.dom.Node root_colors = eElement.getElementsByTagName(property).item(0);
						Element element = (Element) root_colors;
						String red_value = element.getElementsByTagName("R").item(0).getTextContent();
						String green_value = element.getElementsByTagName("G").item(0).getTextContent();
						String blue_value = element.getElementsByTagName("B").item(0).getTextContent();
						int red = Integer.valueOf(red_value);
						int blue = Integer.valueOf(blue_value);
						int green = Integer.valueOf(green_value);
						Integer[] color_values = { red, green, blue };
						color_data.add(color_values);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return color_data;
	}
	
	/**
	 * <h1>Repaint graphics</h1>
	 * 
	 */
	public void update_graphics() {
		core.update_graphics = true;
		core.update.start();
	}
	
	/**
	 * <h1>Saving project to a file</h1>
	 */
	public void save_project() {
		tree_data = "";
		tree_data += current_theme + ":" + themeloc + ":\n";
		tree_data += getWidth() + " ";
		tree_data += getHeight() + " \n";
		tree_data += drag_value_x + " ";
		tree_data += drag_value_y + " \n";
		tree_data += core.scale + " \n";
		tree_data += core.node.text + ":";
		tree_data += core.node.position_x + ":";
		tree_data += core.node.position_y + ":";
		tree_data += core.node.text + ":\n";
		parse_tree(core.node);
		var radcode = new Radcode();
		var path = "";
		/*
		 * File can be auto saved on close. (Currently not used in program).
		 */
		if (autosave) {
			if (home == null) {
				path = radcode.utils.get_home() + "/" + core.node.text + ".mmp";
			} else {
				path = home + core.node.text + ".mmp";
			}
		} else {
			path = file_manager(FileDialog.SAVE, "Save Project");
		}

		radcode.lwriter.create_file(radcode.utils.get_home() + "/.Nodefy.data", home + "\n");
		radcode.lwriter.create_file(path, tree_data);
	}
	
	/**
	 * <h1>Recursive method that parses data from all nodes</h1>
	 * Parses node name, position, parent from all descendants of parameter node.
	 * @Used for saving tree data to a file.
	 * @param node
	 */
	public void parse_tree(Node node) {
		for (Node block : node.child_nodes) {
			tree_data += block.text + ":";
			tree_data += block.position_x + ":";
			tree_data += block.position_y + ":";
			tree_data += node.text + ":\n";
			parse_tree(block);
		}
	}
	
	/**
	 * <h1>Loading a project from file</h1>
	 */
	
	public void load_project() {
		var radcode = new Radcode();
		String file = file_manager(FileDialog.LOAD, "Open Project");
		if (file.equals("nullnull")) {
			return;
		}
		core.node.child_nodes.clear();
		// Parses text to array of string lines.
		String data[] = radcode.lparser.parse_list(file); 
		/*
		 * Parses certain element of data to theme, size, location etc. data.
		 */
		String theme_data[] = radcode.sparser.parse_string(data[0], ':'); // [theme name, path to file]
		String size[] = radcode.sparser.parse_string(data[1], ' '); // [size x, size y]
		setSize(Integer.valueOf(size[0]), Integer.valueOf(size[1]));
		String location[] = radcode.sparser.parse_string(data[2], ' ');
		drag_value_x = Float.valueOf(location[0]);
		drag_value_y = Float.valueOf(location[1]);
		core.transform.setToTranslation(drag_value_x, drag_value_y);
		String zoom[] = radcode.sparser.parse_string(data[3], ' ');
		core.scale = Float.valueOf(zoom[0]);
		var tree = new ArrayList<String>();
		radcode.arraylib.fill(tree, data);
		// Clearing unnecessary data from tree data.
		tree.remove(0); // remove theme data
		tree.remove(0); // remove window size data
		tree.remove(0); // remove location data
		tree.remove(0); // remove zoom data
		// Loading root node.
		String root_node[] = radcode.sparser.parse_string(tree.get(0), ':');
		core.node.text = root_node[0];
		core.node.position_x = Float.valueOf(root_node[1]);
		core.node.position_y = Float.valueOf(root_node[2]);
		tree.remove(0);
		
		/*
		 * Loading process.
		 * 
		 */
		for (String block : tree) {
			String node[] = radcode.sparser.parse_string(block, ':');
			// Loading only root descendants.
			if (node[3].equals(core.node.text)) {
				var child = new Node(Float.valueOf(node[1]), Float.valueOf(node[2]));
				child.text = node[0];
				core.node.child_nodes.add(child);
			} else {
				// Loading all others nodes.
				find_node(core.node, node[3]);
				var child = new Node(Float.valueOf(node[1]), Float.valueOf(node[2]));
				child.text = node[0];
				selected_node.child_nodes.add(child);
			}
		}
		
		current_theme = theme_data[0];
		if (current_theme.equals("default")) {
			set_default_theme();
		} else {
			themeloc = theme_data[1];
			load_theme(themeloc);
		}
		update_graphics();
	}
	
	/**
	 * <h1>FileDialog that returns path to selected file</h1>
	 * @param option Save or Load
	 * @param operation FileDialog Title
	 * @return Path to file
	 */
	public String file_manager(int option, String operation) {
		var file_dialog = new FileDialog(this, operation, option);
		file_dialog.setLocation(getWidth() / 3, getHeight() / 3);
		var radcode = new Radcode();
		if (home != null) {
			/* FileDialog opens in workspace directory.
			 * Workspace path is the latest path of saved/loaded project.
			 */
			String file = radcode.utils.get_home() + "/.Nodefy.data";
			if (radcode.files.file_exists(file)) {
				file_dialog.setDirectory(home); 
			}
		}

		var project_name = core.node.text + ".mmp";

		if (operation.equals("Export PNG")) {
			project_name = core.node.text + ".png";
		}

		if (operation.equals("Export SVG")) {
			project_name = core.node.text + ".svg";
		}

		if (option == FileDialog.SAVE) {
			file_dialog.setFile(project_name);
		}

		file_dialog.setVisible(true);

		if (operation.equals("Export PNG")) {
			home = file_dialog.getDirectory();
			return file_dialog.getDirectory() + file_dialog.getFile();
		}

		if (operation.equals("Export SVG")) {
			home = file_dialog.getDirectory();
			return file_dialog.getDirectory() + file_dialog.getFile();
		}

		if (option == FileDialog.SAVE) {
			home = file_dialog.getDirectory();
			return home + file_dialog.getFile();
		}

		home = file_dialog.getDirectory();
		return home + file_dialog.getFile();
	}
	/** 
	* <h1>Recursive method that search node by mouse position</h1> 
	* Search for certain child node that contains parameter node.
	* @Used for moving, editing and removing the node.
	* 
	*/
	public void find_node(Node node) {
		for (Node block : node.child_nodes) {
			if (mouse_on_block(block)) {
				selected_node = block;
				parent_node = node;
				block_selected = true;
			} else {
				find_node(block);
			}
		}
	}
	/**
	 * <h1>Recursive method that search node by name</h1>
	 * Search for certain child node that contains parameter node.
	 * @Used for loading the tree.
	 * 
	 */
	public void find_node(Node node, String text) {
		for (Node block : node.child_nodes) {
			if (block.text.equals(text)) {
				selected_node = block;
				parent_node = node;
			} else {
				find_node(block, text);
			}
		}
	}
	// Adding node to selected one.
	public void add_node(Node node) {
		selected_node.child_nodes.add(node);
		selected_node = node;
	}

	public void remove_node() {
		parent_node.child_nodes.remove(selected_node);
	}
	
	/**
	 * <h1>Returns true if mouse position in node area</h1>
	 * 
	 */
	public boolean mouse_on_block(Node block) {
		var pos_x = block.position_x + (drag_value_x / core.scale);
		var pos_y = block.position_y + (drag_value_y / core.scale);
		if (mouse_x >= pos_x && mouse_x <= pos_x + block.width && mouse_y >= pos_y && mouse_y <= pos_y + 40) {
			return true;
		}
		return false;
	}
	
	public void update_mouse_position(MouseEvent e) {
		mouse_x = (float) e.getX() / core.scale;
		mouse_y = (float) e.getY() / core.scale;
	}

	public void move_block() {
		selected_node.position_x = mouse_x - offset_x;
		selected_node.position_y = mouse_y - offset_y;
	}
}