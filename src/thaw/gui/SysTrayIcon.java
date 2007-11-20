package thaw.gui;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.event.MouseListener;
import java.awt.PopupMenu;

import thaw.core.Logger;

/**
 * Systray icon that must compile with java 1.4 AND 1.6
 * Limitations:
 * <ul>
 * <li>Will only work with java 1.6 (will do nothing else)</li>
 * <li>Only one icon</li>
 * </ul>
 * <br/>
 */
public class SysTrayIcon {
	public final static int MSG_ERROR = 0;
	public final static int MSG_WARNING = 1;
	public final static int MSG_NONE = 2;
	public final static int MSG_INFO = 3;

	private Object systemTray;
	private Object trayIcon;

	public SysTrayIcon(ImageIcon icon) {
		try {
			systemTray = Class.forName("java.awt.SystemTray").getMethod("getSystemTray", (Class[])null).invoke(null, (Object[])null);
			trayIcon = Class.forName("java.awt.TrayIcon").getConstructor(new Class[] {
				Image.class
			}).newInstance(new Object[] {
				icon.getImage()
			});

			Class.forName("java.awt.TrayIcon").getMethod("setImageAutoSize", new Class[] {
				Boolean.TYPE
			}).invoke(trayIcon, new Object[] {
				new Boolean(true)
			});

		} catch(Exception e) {
			Logger.notice(this, "Can't use Tray icon because: "+e.toString());
			if (e.getCause() != null)
				Logger.notice(this, "Cause: "+e.getCause().toString());
			Logger.notice(this, "Probably due to a JVM without the support for the tray icons");
			systemTray = null;
			trayIcon   = null;
		}
	}

	public boolean canWork() {
		return (systemTray != null);
	}


	public void setVisible(boolean v) {
		if (!canWork())
			return;

		String method = (v ? "add" : "remove");

		try {
			Class.forName("java.awt.SystemTray").getMethod(method, new Class[] {
				Class.forName("java.awt.TrayIcon")
			}).invoke(systemTray, new Object[] {
				trayIcon
			});
		} catch(Exception e) {
			Logger.warning(this, "Error while changing visibility of the icon : "+e.toString());
		}
	}


	public void addMouseListener(MouseListener ml) {
		if (!canWork())
			return;

		try {
			Class.forName("java.awt.TrayIcon").getMethod("addMouseListener", new Class[] {
				MouseListener.class
			}).invoke(trayIcon, new Object[] {
				ml
			});
		} catch(Exception e) {
			Logger.warning(this, "Error while adding mouse listener : "+e.toString());
		}
	}

	public void removeMouseListener(MouseListener ml) {
		if (!canWork())
			return;

		try {
			Class.forName("java.awt.TrayIcon").getMethod("removeMouseListener", new Class[] {
				MouseListener.class
			}).invoke(trayIcon, new Object[] {
				ml
			});
		} catch(Exception e) {
			Logger.warning(this, "Error while removing mouse listener : "+e.toString());
		}
	}

	public void setToolTip(String tt) {
		if (!canWork())
			return;

		try {
			Class.forName("java.awt.TrayIcon").getMethod("setToolTip", new Class[] {
				String.class
			}).invoke(trayIcon, new Object[] {
				tt
			});
		} catch(Exception e) {
			Logger.warning(this, "Error while setting tooltip : "+e.toString());
		}
	}


	public void popMessage(String title, String msg, int msgTypeInt) {
		if (!canWork())
			return;

		try {
			Object type;

			String typeName;

			switch(msgTypeInt) {
			case(MSG_ERROR):   typeName = "ERROR";   break;
			case(MSG_INFO):    typeName = "INFO";    break;
			case(MSG_NONE):    typeName = "NONE";    break;
			case(MSG_WARNING): typeName = "WARNING"; break;
			default:
				Logger.warning(this, "Unknown message type: "+Integer.toString(msgTypeInt));
				return;
			}

			Class messageTypeClass = Class.forName("java.awt.TrayIcon").getClasses()[0];

			type = messageTypeClass.getMethod("valueOf", new Class[] {
				String.class
			}).invoke(null, new Object[] {
				typeName
			});


			Class.forName("java.awt.TrayIcon").getMethod("displayMessage", new Class[] {
				String.class, String.class, type.getClass()
			}).invoke(trayIcon, new Object[] {
				title, msg, type
			});

		} catch(Exception e) {
			Logger.warning(this, "Error while poping up a message: "+e.toString());
		}
	}


	public void setPopupMenu(PopupMenu m) {
		if (!canWork())
			return;

		try {
			Class.forName("java.awt.TrayIcon").getMethod("setPopupMenu", new Class[] {
				PopupMenu.class
			}).invoke(trayIcon, new Object[] {
				m
			});
		} catch(Exception e) {
			Logger.warning(this, "Error while setting popup menu : "+e.toString());
		}
	}


	/**
	 * Return the mouse position on the screen
	 * put here just to keep all the code >= java 1.5 in the same class
	 */
	public java.awt.Point getMousePosition() {
		try {
			Object pointerInfo = Class.forName("java.awt.MouseInfo").getMethod("getPointerInfo", (Class[])null).invoke(null, (Object[]) null);

			java.awt.Point location = (java.awt.Point)Class.forName("java.awt.PointerInfo").getMethod("getLocation", (Class[])null).invoke(pointerInfo, (Object[])null);

			return location;
		} catch(Exception e) {
			Logger.warning(this, "Error while setting popup menu : "+e.toString());
			return null;
		}

	}

}
