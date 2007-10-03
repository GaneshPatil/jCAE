/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007, by EADS France
 
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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.util;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.MGroup3D;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.ArrayList;
import gnu.trove.TIntObjectHashMap;
import org.apache.log4j.Logger;


/**
 * @author cb
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class UNVReader
{

	private static Logger logger=Logger.getLogger(UNVReader.class);
	
	public static void readMesh(Mesh mesh, String file)
	{
		readMesh(mesh, file, -1.0);
	}
	
	public static void readMesh(Mesh mesh, String file, double ridgeAngle)
	{
		TIntObjectHashMap<Vertex> nodesmap = null;
		TIntObjectHashMap<AbstractTriangle> facesmap = null;
		double unit = 1.0;
		String line = "";
		try
		{
			FileInputStream in = new FileInputStream(file);
			BufferedReader rd=new BufferedReader(new InputStreamReader(in));
			while ((line=rd.readLine())!=null)
			{
				if (line.trim().equals("-1"))
				{
					line = rd.readLine();
					if (line.trim().equals("2411") || line.trim().equals("781"))
					{
						// read nodes
						nodesmap = readNodes(mesh, rd, unit);
					}
					else if (line.trim().equals("2412"))
					{
						// read faces
						facesmap = readFace(rd, mesh, nodesmap);
					}
					else if (line.trim().equals("164"))
					{
						// read unit
						unit = readUnit(rd);
					}
					else if ( (line.trim().equals("2430")) || (line.trim().equals("2435")) )
					{
						// read groups
						readGroup(rd, mesh, facesmap);
					}
					else if (line.trim().equals("2414"))
					{
						// read colors
					}
					else
					{
						// default group
						// read end of group
						while (!rd.readLine().trim().equals("-1"))
						{
						}
					}
				}
			}
			in.close();
		}
		catch(Exception e)
		{
				e.printStackTrace();
		}
		if (mesh.hasAdjacency())
		{
			Vertex [] v = new Vertex[nodesmap.size()];
			System.arraycopy(nodesmap.getValues(), 0, v, 0, v.length);
			mesh.buildAdjacency(v, ridgeAngle);
		}
	}

	private static double readUnit(BufferedReader rd)
	{
		double unit = 1.0;
		String line = "";
		try
		{
			//retrieve the second line
			rd.readLine();
			line = rd.readLine();
			
			// fisrt number : the unit
			StringTokenizer st = new StringTokenizer(line);
			String unite = st.nextToken();
			unite = unite.replace('D','E');
			unit = new Double(unite).doubleValue();
			while(!rd.readLine().trim().equals("-1"))
			{
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return unit;
	}

	private static TIntObjectHashMap<Vertex> readNodes(Mesh m, BufferedReader rd, double unit)
	{
		TIntObjectHashMap<Vertex> nodesmap = new TIntObjectHashMap<Vertex>();
		logger.debug("Reading nodes");
		nodesmap.clear();
		double x,y,z;
		String line = "";
		try
		{
			while(!(line=rd.readLine().trim()).equals("-1"))
			{
				//First number : the node's id
				StringTokenizer st = new StringTokenizer(line);
				int index = new Integer(st.nextToken()).intValue();
				line = rd.readLine();
				
				//line contains coord x,y,z
				st = new StringTokenizer(line);
				String x1 = st.nextToken();
				String y1 = st.nextToken();
				String z1;
				try
				{
					z1 = st.nextToken();
				}
				catch (java.util.NoSuchElementException ex)
				{
					z1="0.0";
				}
				
				x1 = x1.replace('D','E');
				y1 = y1.replace('D','E');
				z1 = z1.replace('D','E');
				x = new Double(x1).doubleValue()/unit;
				y = new Double(y1).doubleValue()/unit;
				z = new Double(z1).doubleValue()/unit;
				Vertex n = (Vertex) m.createVertex(x,y,z);
				nodesmap.put(index, n);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		logger.debug("Found "+nodesmap.size()+" nodes");
		return nodesmap;
	}

	private static TIntObjectHashMap<AbstractTriangle> readFace(BufferedReader rd, Mesh mesh, TIntObjectHashMap<Vertex> nodesmap)
	{
		logger.debug("Reading triangles");
		TIntObjectHashMap<AbstractTriangle> facesmap = new TIntObjectHashMap<AbstractTriangle>();
		String line = "";
		boolean quad = false;
		
		try
		{
			while (!(line=rd.readLine().trim()).equals("-1"))
			{
				// first line: type of object
				StringTokenizer st = new StringTokenizer(line);
				String index = st.nextToken();
				String type = st.nextToken();
				int ind = Integer.valueOf(index).intValue();
				if (type.equals("74") || type.equals("91"))
				{
					line=rd.readLine();
					// triangle
					st = new StringTokenizer(line);
					int p1 = Integer.valueOf(st.nextToken()).intValue();
					int p2 = Integer.valueOf(st.nextToken()).intValue();
					int p3 = Integer.valueOf(st.nextToken()).intValue();
					Vertex n1 = nodesmap.get(p1);
					assert n1 != null : p1;
					Vertex n2 = nodesmap.get(p2);
					assert n2 != null : p2;
					Vertex n3 = nodesmap.get(p3);
					assert n3 != null : p3;
					AbstractTriangle f = mesh.createTriangle(n1, n2, n3);
					mesh.add(f);
					n1.setLink(f);
					n2.setLink(f);
					n3.setLink(f);
					// fill the map of faces
					facesmap.put(ind, f);
				}
				else if (type.equals("94"))
				{
					quad = true;
					line=rd.readLine();
					// quadrangle
					st = new StringTokenizer(line);
					int p1 = Integer.valueOf(st.nextToken()).intValue();
					int p2 = Integer.valueOf(st.nextToken()).intValue();
					int p3 = Integer.valueOf(st.nextToken()).intValue();
					int p4 = Integer.valueOf(st.nextToken()).intValue();
					Vertex n1 = nodesmap.get(p1);
					assert n1 != null : p1;
					Vertex n2 = nodesmap.get(p2);
					assert n2 != null : p2;
					Vertex n3 = nodesmap.get(p3);
					assert n3 != null : p3;
					Vertex n4 = nodesmap.get(p4);
					assert n4 != null : p4;
					AbstractTriangle f = mesh.createTriangle(n1, n2, n3);
					mesh.add(f);
					n1.setLink(f);
					n2.setLink(f);
					n3.setLink(f);
					facesmap.put(ind, f);
					f = mesh.createTriangle(n1, n3, n4);
					mesh.add(f);
					n4.setLink(f);
					facesmap.put(-ind, f);
				}
				else
					throw new RuntimeException("Type "+type+" unknown");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		logger.debug("Found "+facesmap.size()+" triangles");
		if (quad)
			logger.error("Quadrangles have been detected and converted into triangles.  If errors occur, convert this UNV file to only use triangles!");
		return facesmap;
	}
	
	private static void readGroup(BufferedReader rd, Mesh mesh, TIntObjectHashMap<AbstractTriangle> facesmap)
	{
		logger.debug("Reading groups");
		String line = "";
		try
		{
			line = rd.readLine();
			while(!line.trim().equals("-1"))
			{
				// read the number of elements to read in the last number of the line
				StringTokenizer st = new StringTokenizer(line);
				/*
				String snb = "";
				// Block number
				st.nextToken();
				while(st.hasMoreTokens())
				{
					snb = st.nextToken();
				}
				// Number of elements
				int nbelem = Integer.valueOf(snb).intValue();
				*/
				// Read group name
				String title = rd.readLine().trim();
				ArrayList<AbstractTriangle> facelist = new ArrayList<AbstractTriangle>();
				// read the group
				while ((line= rd.readLine().trim()).startsWith("8"))
				{
					st = new StringTokenizer(line);
					// read one element over two, the first one doesnt matter
					while(st.hasMoreTokens())
					{
						st.nextToken();
						String index = st.nextToken();
						int ind = Integer.valueOf(index).intValue();
						if (ind != 0)
						{
							facelist.add(facesmap.get(ind));
						}
					}
				}
				new MGroup3D(title, facelist);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
