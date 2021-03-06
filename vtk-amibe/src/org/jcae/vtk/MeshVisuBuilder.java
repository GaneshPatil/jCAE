/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.vtk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.oemm.OEMM;
import  org.jcae.mesh.oemm.OEMM.Node;
import org.jcae.mesh.oemm.TraversalProcedure;

/**
 * This class is an algorithm that creates *V files (where * is the number of the octant)
 * which contains informations for visualisation of the OEMM. The format is very simple :
 * sizeOfTheEdgeIndices
 * arrayEdgeIndices
 * sizeOfTheFreeEdgeIndices
 * arrayFreeEdgesIndices
 * @author Julian Ibarz
 */
public class MeshVisuBuilder extends TraversalProcedure
{
	private static Logger LOGGER=Logger.getLogger(MeshVisuBuilder.class.getName());	
	
	@Override
	public int action(OEMM o, Node c, int octant, int visit)
	{
		if(visit != TraversalProcedure.LEAF)
			return TraversalProcedure.OK;
		
		LOGGER.fine("Making octant " + octant);
		
		MeshVisuReader reader = new MeshVisuReader(o);
		
		LOGGER.finer("Building leaf " + c.leafIndex);
		writeEdges(o, c, reader.buildMeshVisu(c.leafIndex));
		LOGGER.finer("End Building leaf " + c.leafIndex);
		
		return TraversalProcedure.OK;
	}
	protected static File getEdgesFile(OEMM oemm, Node node)
	{
		return new File(oemm.getDirectory(), node.file+"e");
	}
	
	private void writeEdges(OEMM o, Node node, MeshVisuReader.MeshVisu mesh)
	{
		FileChannel fc;
		try
		{
			fc = new FileOutputStream(getEdgesFile(o, node)).getChannel();
		} catch (FileNotFoundException e1)
		{
			LOGGER.severe("I/O error when reading indexed file " + getEdgesFile(o, node));
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
		try
		{
			ArrayList<int[]> allEdges = new ArrayList<int[]>(2);
			allEdges.add(mesh.edges);
			allEdges.add(mesh.freeEdges);

			for (int[] edges : allEdges)
			{
				LOGGER.config("Writing " + (edges.length/2) + " edges");
				ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / 8);
				IntBuffer bufferInteger = bb.asIntBuffer();
				bufferInteger.rewind();
				bufferInteger.put(edges.length);
				bb.rewind();
				fc.write(bb);
				bb = ByteBuffer.allocate((Integer.SIZE / 8) * edges.length);
				bufferInteger = bb.asIntBuffer();
				bufferInteger.rewind();
				bufferInteger.put(edges);
				bb.rewind();
				fc.write(bb);
			}
			
			// Writing fake vertices
			LOGGER.config("Writing " + (mesh.nodes.length / 3) +" fake vertices");
			// Writing the size
			ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / 8);
			IntBuffer bufferInteger = bb.asIntBuffer();
			bufferInteger.rewind();
			bufferInteger.put(mesh.nodes.length);
			bb.rewind();
			fc.write(bb);
			// Writing the vertice
			bb = ByteBuffer.allocate((Float.SIZE / 8) * mesh.nodes.length);
			FloatBuffer bufferFloat = bb.asFloatBuffer();
			bufferFloat.rewind();
			bufferFloat.put(mesh.nodes);
			bb.rewind();
			fc.write(bb);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "Error in saving to " + getEdgesFile(o, node), e);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally
		{
			try
			{
				fc.close();
			}
			catch (IOException e)
			{
				//ignore this
			}
		}
	}
}
