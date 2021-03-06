/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007,2008,2009, by EADS France

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh.bora.ds;

import org.jcae.mesh.bora.algo.AlgoInterface;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;

import java.util.logging.Logger;
import org.jcae.mesh.cad.CADShapeEnum;

public class BDiscretization
{
	private static final Logger LOGGER = Logger.getLogger(BDiscretization.class.getName());

	private final BCADGraphCell graphCell;
	// List of set of BSubMesh instances containing this BDiscretization.
	private final Collection<BSubMesh> submesh = new LinkedHashSet<BSubMesh>();
	private Constraint constraint;
	private AlgoInterface algo;
	private boolean computed = false;
	private Object mesh;

	// Unique identitier
	private int id = -1;
	static int nextId = -1;

	BDiscretization(BCADGraphCell g, Constraint c)
	{
		assert g.getType() == CADShapeEnum.EDGE ||
			g.getType() == CADShapeEnum.FACE ||
			g.getType() == CADShapeEnum.VERTEX:
			"Cannot create a BDiscretization of type "+g.getType();
		// Store forward oriented cell
		if (g.getOrientation() != 0 && g.getReversed() != null)
			graphCell = g.getReversed();
		else
			graphCell = g;
		constraint = c;
		setId();
	}

	private void setId()
	{
		nextId++;
		id = nextId;
	}

	public final int getId()
	{
		return id;
	}

	public final BCADGraphCell getGraphCell()
	{
		return graphCell;
	}

	public final Constraint getConstraint()
	{
		return constraint;
	}

	public final Collection<BSubMesh> getSubmesh()
	{
		return submesh;
	}

	final void addSubMesh(BSubMesh s)
	{
		submesh.add(s);
	}

	void removeSubMesh(BSubMesh s)
	{
		submesh.remove(s);
	}

	public final BSubMesh getFirstSubMesh()
	{
		return submesh.iterator().next();
	}

	/**
	 * When combining two constraints on the same BCADGraphCell (this happens during
	 * the phase when the set of needed BDiscretization are created), we want to
	 * ensure that those two constraints are not both "original" constraints
	 * set by the user on the same object of the CAD.
	 */
	final void combineConstraint(BDiscretization baseDiscr)
	{
		Constraint newCons = baseDiscr.constraint.createInheritedConstraint(graphCell, constraint);
		if (constraint != null)
		{
			Constraint baseOrigCons = baseDiscr.constraint.originConstraint(graphCell);
			Constraint localOrigCons = constraint.originConstraint(graphCell);
			// situation where there are two conflicting user
			// constraints for the same discretization
			if (((localOrigCons != null) && (baseOrigCons != null)) && (localOrigCons != baseOrigCons))
				throw new RuntimeException("Definition of model imposes the same discretization for shape "+graphCell.getShape()+" but there are two different user-defined constraints for this discretization:" + baseOrigCons + " and " + localOrigCons );

			if (!newCons.getHypothesis().combine(constraint.getHypothesis()))
				throw new RuntimeException("Cannot combine "+newCons+" with "+constraint+" on "+graphCell);
			// TODO: in combine(), it will be necessary to detect 
			// which is the original constraint
		} 
		constraint = newCons;
	}

	final void addAllSubMeshes(BDiscretization parent)
	{
		submesh.addAll(parent.submesh);
	}

	/**
	 * Check whether a <code>BSubMesh</code> is already present.
	 * @param s <code>BSubMesh</code> being checked.
	 * @return <code>true</code> if <code>BSubMesh</code> is already
	 * found, <code>false</code> otherwise.
	 */
	final boolean contains(BSubMesh s)
	{
		return submesh.contains(s);
	}

	/**
	 * Test of inclusion of the submesh list in the submesh list of that.
	 * Check whether a <code>BDiscretization</code> has all of its
	 * submesh list contained in the parameter's submesh list
	 * @param that  object being checked.
	 */
	public final boolean contained(BDiscretization that)
	{
		for (BSubMesh s : submesh)
		{
			if (!(that.submesh.contains(s)))
				return false;
		}
		return true;
	}

	/**
	 * Check whether a <code>BDiscretization</code> instance contains
	 * at least a common <code>BSubMesh</code>.
	 * @param that  object being checked.
	 * @return <code>false</code> if a <code>BSubMesh</code> is common to
	 * both sets, <code>true</code> otherwise.
	 */
	final boolean emptyIntersection(BDiscretization that)
	{
		for (BSubMesh s : that.submesh)
		{
			if (submesh.contains(s))
				return false;
		}
		return true;
	}

	/**
	 * Check whether a <code>BDiscretization</code> instance is needed for 
	 * the definition of a <code>BSubMesh</code>.
	 */
	public final boolean isSubmeshChild(BSubMesh that)
	{
		// if the submesh is not contained in the submesh list, there is no need
		// to continue further
		if (!submesh.contains(that))
			return false;
		// loop on the constraints of the submesh
		for (Constraint cons : that.getConstraints())
		{
			BCADGraphCell cell = cons.getGraphCell();
			// loop on the childs of the graphcell of the constraint of the same type
			for (Iterator<BCADGraphCell> it = cell.shapesExplorer(graphCell.getType()); it.hasNext(); )
			{
				BCADGraphCell child = it.next();
				// loop on the discretizations of the child
				for (BDiscretization discr : child.getDiscretizations())
				{
					if (discr == this)
					    return true;
				}
			}

		}
		return false;
	}

	public final Object getMesh()
	{
		return mesh;
	}

	public final void setMesh(Object m)
	{
		mesh = m;
	}

	final void discretize()
	{
		if (computed)
			return;
		if (algo == null)
			algo = constraint.getHypothesis().findAlgorithm(graphCell.getType());
		if (algo == null || !algo.isAvailable())
			return;
		if (!algo.compute(this))
			LOGGER.warning("Failed! "+algo);
		computed = true;
	}

	@Override
	public final String toString()
	{
		String ret = "Discretization: "+id;
		ret += " (cons. "+constraint+") "+submesh;
		return ret;
	}
}
