/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.gef.examples.logicdesigner.model.commands;

import java.util.*;

import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.examples.logicdesigner.LogicMessages;
import org.eclipse.gef.examples.logicdesigner.model.*;

public class CloneCommand
	extends org.eclipse.gef.commands.Command
{

private List parts;
private LogicDiagram parent;
private List newParts;
private Map bounds;
private Map indexs;

public CloneCommand() {
	super(LogicMessages.CloneCommand_Label);
	newParts = new LinkedList();
	parts = new LinkedList();
}

public void addPart(LogicSubpart part, Rectangle newBounds) {
	parts.add(part);
	if (bounds == null) {
		bounds = new HashMap();
	}
	bounds.put(part, newBounds);
}

public void addPart(LogicSubpart part, int index) {
	parts.add(part);
	if (indexs == null) {
		indexs = new HashMap();
	}
	indexs.put(part, new Integer(index));
}

protected void clonePart(LogicSubpart oldPart, LogicDiagram newParent, Rectangle newBounds,
					  List newConnections, Map connectionPartMap, int index) {
	LogicSubpart newPart = null;
	
	if (oldPart instanceof AndGate) {
		newPart = new AndGate();
	} else if (oldPart instanceof Circuit) {
		newPart = new Circuit();
	} else if (oldPart instanceof GroundOutput) {
		newPart = new GroundOutput();
	} else if (oldPart instanceof LED) {
		newPart = new LED();
		newPart.setPropertyValue(LED.P_VALUE, oldPart.getPropertyValue(LED.P_VALUE));
	} else if (oldPart instanceof LiveOutput) {
		newPart = new LiveOutput();
	} else if (oldPart instanceof LogicLabel) {
		newPart = new LogicLabel();
		((LogicLabel)newPart).setLabelContents(((LogicLabel)oldPart).getLabelContents());
	} else if (oldPart instanceof OrGate) {
		newPart = new OrGate();
	} else if (oldPart instanceof LogicFlowContainer) {
		newPart = new LogicFlowContainer();
	} else if (oldPart instanceof XORGate) {
		newPart = new XORGate();
	}
	
	if (oldPart instanceof LogicDiagram) {
		Iterator i = ((LogicDiagram)oldPart).getChildren().iterator();
		while (i.hasNext()) {
			// for children they will not need new bounds.
			clonePart((LogicSubpart)i.next(), (LogicDiagram)newPart, null, 
					newConnections, connectionPartMap, -1);
		}
	}
	
	Iterator i = oldPart.getTargetConnections().iterator();
	while (i.hasNext()) {
		Wire connection = (Wire)i.next();
		Wire newConnection = new Wire();
		newConnection.setValue(connection.getValue());
		newConnection.setTarget(newPart);
		newConnection.setTargetTerminal(connection.getTargetTerminal());
		newConnection.setSourceTerminal(connection.getSourceTerminal());
		newConnection.setSource(connection.getSource());
	
		Iterator b = connection.getBendpoints().iterator();
		Vector newBendPoints = new Vector();
		
		while (b.hasNext()) {
			WireBendpoint bendPoint = (WireBendpoint)b.next();
			WireBendpoint newBendPoint = new WireBendpoint();
			newBendPoint.setRelativeDimensions(bendPoint.getFirstRelativeDimension(), 
					bendPoint.getSecondRelativeDimension());
			newBendPoint.setWeight(bendPoint.getWeight());
			newBendPoints.add(newBendPoint);
		}
		
		newConnection.setBendpoints(newBendPoints);
		newConnections.add(newConnection);
	}
	
	
	if (index < 0) {
		newParent.addChild(newPart);
	} else {
		newParent.addChild(newPart, index);
	}
	
	newPart.setSize(oldPart.getSize());

	
	if (newBounds != null) {
		newPart.setLocation(newBounds.getTopLeft());
	} else {
		newPart.setLocation(oldPart.getLocation());
	}
	
	// keep track of the new parts so we can delete them in undo
	// keep track of the oldpart -> newpart map so that we can properly attach
	// all connections.
	newParts.add(newPart);
	connectionPartMap.put(oldPart, newPart);
}

public void execute() {
	Map connectionPartMap = new HashMap();
	List newConnections = new LinkedList();

	Iterator i = parts.iterator();
	
	LogicSubpart part = null;
	while (i.hasNext()) {
		part = (LogicSubpart)i.next();
		if (bounds != null && bounds.containsKey(part)) {
			clonePart(part, parent, (Rectangle)bounds.get(part), 
					newConnections, connectionPartMap, -1);	
		} else if (indexs != null && indexs.containsKey(part)) {
			clonePart(part, parent, null, newConnections, 
					connectionPartMap, ((Integer)indexs.get(part)).intValue());
		} else {
			clonePart(part, parent, null, newConnections, connectionPartMap, -1);
		}
	}
	
	// go through and set the source of each connection to the proper source.
	Iterator c = newConnections.iterator();
	
	while (c.hasNext()) {
		Wire conn = (Wire)c.next();
		LogicSubpart source = conn.getSource();
		if (connectionPartMap.containsKey(source)) {
			conn.setSource((LogicSubpart)connectionPartMap.get(source));
			conn.attachSource();
			conn.attachTarget();
		}
	}	
}



public void setParent(LogicDiagram parent) {
	this.parent = parent;
}

public void redo() {
	execute();
}

protected void removePart(LogicSubpart part, LogicDiagram parent) {
	if (part instanceof LogicDiagram) {
		Iterator i = ((LogicDiagram)part).getChildren().iterator();
		while (i.hasNext()) {
			removePart((LogicSubpart)i.next(), (LogicDiagram)parent);
		}
	}
	parent.removeChild(part);
}

public void undo() {
	Iterator i = newParts.iterator();
	while (i.hasNext()) {
		removePart((LogicSubpart)i.next(), parent);
	}
	
}

}