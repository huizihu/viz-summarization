package lattice;
/*
 * @author himeldev
 */
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import org.w3c.dom.NodeList;

import algorithms.Traversal;
import distance.Distance;
import distance.Euclidean;

public class Hierarchia 
{
	public static String datasetName;
	public static Database db;
	public static ArrayList<String> attribute_names;
	public static HashMap<String, ArrayList<String>> uniqueAttributeKeyVals;
	public static String xAxis;
	public Hierarchia(String datasetName, String xAxis) throws SQLException {
		Hierarchia.datasetName = datasetName;
		Hierarchia.xAxis=xAxis;
		Hierarchia.db =  new Database();
		Hierarchia.attribute_names = get_attribute_names();
		Hierarchia.uniqueAttributeKeyVals = populateUniqueAttributeKeyVals();
	}
	public static ArrayList<String> getAttribute_names() {
		return attribute_names;
	}
	public static void setAttribute_names(ArrayList<String> attribute_names) throws SQLException {
		Hierarchia.attribute_names = attribute_names;
		Hierarchia.uniqueAttributeKeyVals = populateUniqueAttributeKeyVals();
	}
	public static HashMap<String, ArrayList<String>> populateUniqueAttributeKeyVals() throws SQLException{
		HashMap<String, ArrayList<String>> uniqueAttributeKeyVals =new HashMap<String, ArrayList<String>>();
		for (int i=0;i<attribute_names.size();i++) {
			String key = attribute_names.get(i);
			ArrayList<String> attrVals = 
					Database.resultSet2ArrayStr(Database.findDistinctAttrVal(key, datasetName));
			uniqueAttributeKeyVals.put(key, attrVals);			
		}
		return uniqueAttributeKeyVals;
	}
    public static Lattice generateFullyMaterializedLattice(Distance distance, double iceberg_ratio, double informative_criteria){
    	/**
    	 * Fully Materialize Visualization Lattice by populating id2MetricMap, nodeList, id2IDMap 
    	 * @param distance: Distance function used for computing edge weights between parent & child
    	 * iceberg_ratio: enforces that all nodes to be added into the lattice must be > x % of size of the root node
    	 * informative_criteria: for a parent to be added to the lattice, it must be within x % the minimum distance to be declared an "informative parent" 
    	 */
    	    System.out.println("---------------- Generate Fully Materialized Lattice -----------------");
    	    System.out.println("Informative parent defined as parents within "+informative_criteria*100+"% the minimum.");
        HashMap<String, ArrayList<Double>> map_id_to_metric_values = new HashMap<String, ArrayList<Double>>();
        ArrayList<Node> node_list = new ArrayList<Node>();// node_list: list of child indexes       
        System.out.println("uniqueAttributeKeyVals:"+uniqueAttributeKeyVals);
        HashMap<String, Integer> map_id_to_index = new HashMap<String, Integer>();
        Node root = new Node("#");
        ArrayList<Double> root_measure_values = compute_visualization(root,new ArrayList<String>(),new ArrayList<String>());
        //ArrayList<Double> root_measure_values = new ArrayList<Double>(); 
		//root_measure_values.add(46.0);
		//root_measure_values.add(64.0);
        System.out.println("Root measure:"+root_measure_values);
        long rootSize = root.getPopulation_size();
        System.out.println("Root size:"+rootSize);
        double  min_iceberg_support = iceberg_ratio*rootSize;
		System.out.println("Minimum Iceberg support:"+min_iceberg_support);
        map_id_to_metric_values.put("#", root_measure_values);
        node_list.add(root);
        map_id_to_index.put("#", 0);
        ArrayList <String> attribute_combination = new ArrayList<String>(); // List of attribute combination that excludes the xAxis item
        attribute_combination.addAll(attribute_names); // Deep copy original attribute names
        attribute_combination.remove(xAxis); // remove the xAxis item in the attribute list
        int n = attribute_names.size();
        for(int k = 1; k <= n; k++) // k-attribute combination
        {
        		//System.out.println("k: "+k);
            ArrayList<ArrayList<String>> k_attribute_combinations = new ArrayList<ArrayList<String>>();
            ArrayList<String> current_combination = new ArrayList<String>();
            for(int i = 0; i < k; i++)
            {
                current_combination.add("#");
            }
            /*
	            System.out.println("current_combination:"+current_combination);
	            current_combination:[#]
				current_combination:[#, #]
				current_combination:[#, #, #]
				current_combination:[#, #, #, #]
				current_combination:[#, #, #, #, #]
				current_combination:[#, #, #, #, #, #]
				current_combination:[#, #, #, #, #, #, #]
            */
            generate_k_combinations(attribute_combination, k, 0, current_combination, k_attribute_combinations);
            //System.out.println("Number of combinations: "+k_attribute_combinations.size());
            
            //System.out.println("Attribute Combinations: "+k_attribute_combinations);
            for(int i = 0; i < k_attribute_combinations.size(); i++) // Looping through the i-th item in the k-attribute combination
            {
                current_combination = k_attribute_combinations.get(i);
//                System.out.println(current_combination);
//                System.out.println(current_combination.get(0));
                ArrayList<ArrayList<String>> attribute_values = new ArrayList<ArrayList<String>>();
                for(int j = 0; j < k; j++)
                {
//                		System.out.println(j);
//                		System.out.println(current_combination.get(j));
//                		ArrayList<String> possibleAttrVals = 
//                				db.resultSet2ArrayStr(Database.findDistinctAttrVal(current_combination.get(j), datasetName));
                		ArrayList<String> possibleAttrVals = uniqueAttributeKeyVals.get(current_combination.get(j));
//                    ArrayList<String> binary_values = new ArrayList<String>();
//                    binary_values.add("0");
//                    binary_values.add("1");
//                    attribute_values.add(binary_values);
                    attribute_values.add(possibleAttrVals);       
                }
                ArrayList<ArrayList<String>> value_permutations = new ArrayList<ArrayList<String>>();
                ArrayList<String> current_permutation = new ArrayList<String>();
                for(int j = 0; j < k; j++)
                {
                    current_permutation.add("#");
                }
                generate_value_permutations(attribute_values, 0, current_permutation, value_permutations);
//                System.out.println("Value Permutations: "+value_permutations);
                
                for(int j=0; j < value_permutations.size(); j++)
                {
                    current_permutation = value_permutations.get(j);
                    String visualization_key = "#";
                    for(int sp = 0; sp < k; sp++)
                    {
                        visualization_key += current_combination.get(sp)+"$"+current_permutation.get(sp)+"#";  
                    }
                    
//                    ArrayList<Double> measure_values = compute_visualization(new Node("#"),current_combination, current_permutation);
                    Node node = new Node(visualization_key);
                    ArrayList<Double> current_visualization_measure_values = compute_visualization(node,current_combination, current_permutation);
                    
                    if (node.getPopulation_size()>= min_iceberg_support) {
	                    	if(current_visualization_measure_values.get(0) > 0.0 || current_visualization_measure_values.get(1)> 0.0 )
	                        {
	                            //System.out.println("Current Visualization: "+visualization_key+" -- "+measure_values);
	                            //System.out.print("C");
	                            map_id_to_metric_values.put(visualization_key, current_visualization_measure_values);
	                            node_list.add(node);
	                            map_id_to_index.put(visualization_key, node_list.size()-1);
	
	                            double min_distance = 1000000;
	                            for(int dr = 0; dr < k; dr++)
	                            {
	                                visualization_key = "#";
	                                for(int sp = 0; sp < k; sp++)
	                                {
	                                    if(sp!=dr)
	                                        visualization_key += current_combination.get(sp)+"$"+current_permutation.get(sp)+"#";
	                                }
	                                //System.out.println("Potential parent: "+visualization_key+" -- "+map_id_to_metric_values.get(visualization_key));
	                                if(map_id_to_metric_values.get(visualization_key) != null)
	                                {
	                                    ArrayList<Double> parent_visualization_measure_values = map_id_to_metric_values.get(visualization_key);
	                                    double [] cviz = Traversal.ArrayList2Array(current_visualization_measure_values);
	                                		double [] pviz =  Traversal.ArrayList2Array(parent_visualization_measure_values);
	                                    double dist = distance.computeDistance(cviz,pviz);
	                                    if(dist < min_distance)
	                                        min_distance = dist;
	                                }
	                            }
	                            
	                            for(int dr = 0; dr < k; dr++)
	                            {
	                                visualization_key = "#";
	                                for(int sp = 0; sp < k; sp++)
	                                {
	                                    if(sp!=dr)
	                                        visualization_key += current_combination.get(sp)+"$"+current_permutation.get(sp)+"#";
	                                }
	                                
	                                if(map_id_to_metric_values.get(visualization_key) != null)
	                                {
	                                    ArrayList<Double> parent_visualization_measure_values = map_id_to_metric_values.get(visualization_key);
	                                    double dist = compute_distance(current_visualization_measure_values, parent_visualization_measure_values);
	                                    //System.out.println("dist criteria:"+min_distance/informative_criteria);
	                                    if(dist*informative_criteria <= min_distance)
	                                    {
	                                        int parent_index = map_id_to_index.get(visualization_key);
	                                        
	                                        ArrayList<Integer> child_list = node_list.get(parent_index).get_child_list();
	                                        if (parent_index!=node_list.size()-1) {
	                                        		child_list.add(node_list.size()-1);
	                                        }
	                                        node_list.get(parent_index).set_child_list(child_list);
	                                        ArrayList<Double> dist_list = node_list.get(parent_index).get_dist_list();
	                                        dist_list.add(dist);
	                                        //System.out.println("Informative parent: "+visualization_key+" -- "+dist);
	                                    }/*else {
	                                    		System.out.println("Non-informative parent:"+visualization_key+" -- "+dist);
	                                    }*/
	                                }
	                            }
	                        }
                    		}
                    
                    /*
                    System.out.print("Node List: ");
                    for(int x = 0; x < node_list.size(); x++)
                    {
                        System.out.print(node_list.get(x).get_id()+" ");
                    }
                    System.out.println();
                    */
                    /*
                    for(int x = 0; x < node_list.size(); x++)
                    {
                        System.out.println("Node"+x+": "+node_list.get(x).get_id());
                        for(int y=0; y < node_list.get(x).get_child_list().size(); y++)
                        {
                            System.out.print(node_list.get(x).get_child_list().get(y)+" ");
                        }
                        System.out.println();
                    }
                    */
                    
                }
            }            
        }
        /*
        // Printing out materialized outputs 
        for(int x = 0; x < node_list.size(); x++)
        {
            System.out.println("Node index: "+x+", Node id: "+node_list.get(x).get_id());
            System.out.println("Parents of "+node_list.get(x).get_child_list().size()+" nodes:");
            for(int y=0; y < node_list.get(x).get_child_list().size(); y++)
            {
                System.out.println(node_list.get(node_list.get(x).get_child_list().get(y)).get_id()+" "
                        +node_list.get(x).get_dist_list().get(y));
            }
            System.out.println();
        }*/
        Lattice materialized_lattice = new Lattice(map_id_to_metric_values,node_list,map_id_to_index);
        //System.out.println("------------------------------------------------ ");
        return materialized_lattice;
    }
    static ArrayList<String> get_attribute_names()
    {
        ArrayList<String> attribute_names = new ArrayList<String>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(datasetName+".csv"));
            String line = null;
            if((line = reader.readLine()) != null) 
            {
                String [] names = line.split(",");
                //System.out.println(Arrays.toString(names));
                for(int i = 0; i < names.length-1; i++)
                {
                    attribute_names.add(names[i]);
                }
            }
        }
        catch(IOException e)
        {
            System.out.println("Error in get_attribute_names()");
            System.out.println("attribute_names:"+attribute_names);
        }
        return attribute_names;
    }
    
    static void generate_k_combinations(ArrayList<String> attribute_names, int len, int start, ArrayList<String> current_combination, 
            ArrayList<ArrayList<String>> k_combination_list)
    {
        if (len == 0)
        {
            ArrayList<String> current_combination_copy = new ArrayList<String>(current_combination);
            k_combination_list.add(current_combination_copy);
            return;
        }
        for (int i = start; i <= attribute_names.size()-len; i++)
        {
            current_combination.set(current_combination.size() - len, attribute_names.get(i));
            generate_k_combinations(attribute_names, len-1, i+1, current_combination, k_combination_list);
        }
    }
    static void generate_value_permutations(ArrayList<ArrayList<String>> attribute_values, int depth,
            ArrayList<String> current_permutation, ArrayList<ArrayList<String>> value_permutations)
    {
        if(depth == attribute_values.size())
        {
            ArrayList<String> current_permutation_copy = new ArrayList<String>(current_permutation);
            value_permutations.add(current_permutation_copy);
            return;
        }
        
        for(int i = 0; i < attribute_values.get(depth).size(); ++i)
        { 
            current_permutation.set(depth, attribute_values.get(depth).get(i));
            generate_value_permutations(attribute_values, depth + 1, current_permutation, value_permutations);
        }
    }
    public static ArrayList<Double> compute_visualization(Node node, ArrayList<String> current_combination, ArrayList<String> current_permutation)
    {
    		int  numXaxis = uniqueAttributeKeyVals.get(xAxis).size();
    		//System.out.println("Attribute-Value Combination:"+current_combination+" -- "+current_permutation);
    		
    	    //Attribute-Value Combination:[cap_shape , cap_surface , cap_color ] -- [f, s, g]
        ArrayList<Double> measure_values = new ArrayList<Double>();
        ArrayList<Double> normalized_measure_values = new ArrayList<Double>();
        ArrayList<Integer> attribute_positions = new ArrayList<Integer>();
        //System.out.println("attribute_names:"+ attribute_names);
        for(int i = 0; i < current_combination.size(); i++)
        {
            for(int j = 0; j < attribute_names.size(); j++)
            {
                if(current_combination.get(i).equals(attribute_names.get(j)))
                {
                    attribute_positions.add(j);
                }
            }
        }
//        System.out.println("----");
//        System.out.println("attribute_positions:"+attribute_positions);
//        System.out.println("current_permutation:"+current_permutation);
        
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(datasetName+".csv"));
            String line = reader.readLine();
            
    			for (int iattr=0;iattr< numXaxis;iattr++) {
    				measure_values.add(0.0); //initialize summation
    			}
            
            
//            for (int icol =0 ; icol<attribute_names.size() ; icol++) {
//	        		System.out.println(attribute_names.get(icol));
//	        		System.out.println(uniqueAttributeKeyVals.get(attribute_names.get(icol)));
//	        		System.out.println(measure_values.get(icol)+",");	
//	        }
            
            
            while((line = reader.readLine()) != null) 
            {
            		// Checking if condition is satisfied for all columns
            		// if any column does not satisfied, then flag=1;
                String [] values = line.split(",");
                
                int flag = 1;
                for(int i = 0; i < attribute_positions.size(); i++)
                {
                    if(!values[attribute_positions.get(i)].equals(current_permutation.get(i)))
                    {
                        flag = 0;
                        break;
                    }
                }
                /* Printing out the value (list of rows) that satisfies the conditions
                 * if (flag==1) {
                		System.out.println("values:"+Arrays.toString(values));
                }*/
                int xidx =attribute_names.indexOf(xAxis);
//                System.out.println(xAxis);
//                System.out.println(xidx);
                //for (int ii =0;ii <attribute_positions.size();ii ++) {
                //		ArrayList<String> attrVals = uniqueAttributeKeyVals.get(attribute_names.get(attribute_positions.get(ii)));
                		ArrayList<String> attrVals = uniqueAttributeKeyVals.get(xAxis);
                		for (int j =0;j <numXaxis;j ++){
                			//System.out.println(attribute_names.get(j));
                			String attr = attrVals.get(j);
                			//System.out.println("attr:"+attr);
                			//System.out.println(values[values.length-2]);
                			if(flag == 1 && values[xidx].equals(attr)) {
                				//System.out.println("inside");
                				//System.out.println("Before:"+measure_values.get(i).get(j));
                				double measure_val;
                				measure_val = Double.parseDouble(values[values.length-1]);
                				//System.out.println(measure_val);
                				measure_values.set(j, measure_values.get(j)+measure_val);
                				//System.out.println("After:"+measure_values.get(i).get(j));
                				//measure_values.set(i, measure_values.get(i).get(j)+measure_val);
                            //sum_0 += Double.parseDouble(values[values.length-1]);
                			}
                				
                		}
	    				//measure_values.get(icol).add(0.0); //initialize summation
                		//}
//                if(flag == 1 && values[values.length-2].equals("0"))
//                    sum_0 += Double.parseDouble(values[values.length-1]);
//                else if(flag == 1 && values[values.length-2].equals("1"))
//                    sum_1 += Double.parseDouble(values[values.length-1]);
//                    
            }
            reader.close();
        }
        catch(IOException e)
        {
            System.out.println("IOException in compute_visualization Error");
            System.out.println("measure_values:"+measure_values);
        }
        /*
        double Min = 1.0;
        double Max = 99.0;
        double x = Min + (Math.random() * ((Max - Min) + 1));
        measure_values.add(x);
        measure_values.add(100-x);
        */
//        for (int i =0;i <attribute_positions.size();i ++) {
//	    		ArrayList<String> attrVals = uniqueAttributeKeyVals.get(attribute_names.get(attribute_positions.get(i)));

			
        		long denominator = 0;
	    		for (int j =0;j <numXaxis;j ++){
	    			denominator += measure_values.get(j);
	    		}
	    		//System.out.println("denominator:"+denominator);
	    		for (int j =0;j <numXaxis;j ++){
	    			if (Math.abs(denominator)>0.000001) {
	    				normalized_measure_values.add(measure_values.get(j)/denominator*100);
	    				//System.out.println("measure_values:"+measure_values);
	    		        //System.out.println("normalized_measure_values:"+normalized_measure_values);
	    			}else {
	    				normalized_measure_values.add(-1.0);
	    			}
	    		}
	    		node.setPopulation_size(denominator);
        return normalized_measure_values;
    }
    
    public static void print_map(Map mp) 
    {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) 
        {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
    }

    
    static double compute_distance(ArrayList<Double> l1, ArrayList<Double> l2)
    {
        double distance = 0;
        for(int i=0; i < l1.size() && i < l2.size(); i++)
        {
            distance += (l1.get(i)-l2.get(i))*(l1.get(i)-l2.get(i));
            //distance += Math.abs(l1.get(i)-l2.get(i));
        }
        return Math.sqrt(distance);
//        return distance;
    }
    public static void mergeNodes(Lattice lattice) {
    		/**
    		 *  Merge nodes is a preprocessing step that is done after the lattice is generated
    		 *  before the traversal. This resembles the compression of C-cube, where the cube 
    		 *  has no cells with descendants that have the same measure values. This function merges
    		 *  all the nodes that have identical values who are siblings into one node with concatenated.
    		 */
    	    System.out.println("---------------- Merging Nodes -----------------");
		HashMap<ArrayList<Double>,ArrayList<String>> val2IDsMap=new HashMap<ArrayList<Double>,ArrayList<String>>();
		for (Node node : lattice.nodeList) {
			String id = node.get_id();
			ArrayList<Double> value = lattice.id2MetricMap.get(node.get_id());
			//System.out.println(value);
			ArrayList<String> IDs = val2IDsMap.get(value);
			if (IDs!=null) {
				// exist previous entry, add to previous list
				val2IDsMap.get(value).add(id);
			}else {
				// No previous entry, add new array list with that ID
				val2IDsMap.put(value, new ArrayList<String>(Arrays.asList(id)));
			}
		}
		//Hierarchia.print_map(val2IDsMap);
		Iterator it = val2IDsMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        ArrayList<Double> k = (ArrayList<Double>) pair.getKey();
	        ArrayList<String> v = (ArrayList<String>) pair.getValue();
	        if (v.size()>1) {
	        		// Merge nodes which has more than one same-value node
	        		ArrayList<String> nodes2merge = v; // Note later we will have better strategies for choosing which nodes to merge (e.g. only descendants, only siblings, etc)
	        		mergeNodesInLattice(lattice,nodes2merge);
	        }
	        //System.out.println(k+ " =: " + v);
	    }
//	    System.out.println("---------------- Done Merging Nodes -----------------");
	}
    public static void mergeNodesInLattice(Lattice lattice,ArrayList<String> nodes2merge) {
    		/* 
    		 * INCOMPLETE STILL NEED DEBUGGING!!!
    		 * Given a list of nodes keys to merge, merge the nodes in the lattice
    		 */
//    		System.out.println("--mergeNodesInLattice--");
//    		System.out.println("nodes2merge:"+nodes2merge);
    		
    		int nonNullIdx = 0;
    		while (lattice.id2IDMap.get(nodes2merge.get(nonNullIdx))==null) {
    			nonNullIdx+=1;
    		}
//    		System.out.println("lattice.id2IDMap.get(nodes2merge.get(nonNullIdx)):"+lattice.id2IDMap.get(nodes2merge.get(nonNullIdx)));
    		Node keepNode= lattice.nodeList.get(lattice.id2IDMap.get(nodes2merge.get(nonNullIdx)));
    		ArrayList<String> nodes2remove = new ArrayList<String>();
    		ArrayList<Integer> nodeIDs2remove = new ArrayList<Integer>();
		for (int i =0; i<nodes2merge.size();i++) {
			String nodeKey = nodes2merge.get(i);
			int nodeID = lattice.id2IDMap.get(nodeKey);
			if (i==nonNullIdx) {
				keepNode = lattice.nodeList.get(nodeID);
				// For each nodeKeys, chose the first one to retain and accumulate all the filter names
				keepNode.setMerged_nodes_keys(new ArrayList<String>(Arrays.asList(nodeKey)));
			}else {
				keepNode.getMerged_nodes_keys().add(nodeKey);
//				System.out.println("nodeID:"+nodeID);
//				System.out.println("node:"+lattice.nodeList.get(nodeID));
				keepNode.get_child_list().addAll(lattice.nodeList.get(nodeID).get_child_list());
				keepNode.get_dist_list().addAll(lattice.nodeList.get(nodeID).get_dist_list());
				//lattice.nodeList.remove(nodeID);
				lattice.id2MetricMap.remove(nodeKey);
				nodes2remove.add(nodeKey);
				nodeIDs2remove.add(nodeID);
			}
		}
//		System.out.println("Where is cap color =b:"+lattice.id2IDMap.get("#cap_color$b#"));
		// 1. Remove elements in nodes2remove in nodeList
//		System.out.println("Before:"+lattice.nodeList.size());
//		System.out.println("nodeIDs2remove:"+nodeIDs2remove);
//		System.out.println("nodes2remove:"+nodes2remove);
		for(int x = nodeIDs2remove.size()-1 ; x > 0; x--)
		{
//			System.out.println("x:"+x);
//			System.out.println(nodeIDs2remove.get(x));
			lattice.nodeList.remove((int) nodeIDs2remove.get(x));// remove by index
//			System.out.println("Removed:"+nodeIDs2remove.get(x));
		}
//		System.out.println("After:"+lattice.nodeList.size());
//		System.out.println("Where is cap color =b:"+lattice.id2IDMap.get("#cap_color$b#"));
		// 2. rebuild id2IDMap according to the new nodeList
//		System.out.println("Before lattice.id2IDMap.size():"+lattice.id2IDMap.size());
		lattice.id2IDMap.clear();
		for (int i=0; i<lattice.nodeList.size();i++) {
			lattice.id2IDMap.put(lattice.nodeList.get(i).get_id(),i);
		}
//		print_map(lattice.id2IDMap);
//		System.out.println("id2IDMap size:"+lattice.id2IDMap.size());
    }
    public static void main(String[] args) throws SQLException, FileNotFoundException, UnsupportedEncodingException 
    {
//    		Hierarchia h = new Hierarchia("turn","has_list_fn");
//    		Node placeholderNode = new Node("#");
//    		compute_visualization(placeholderNode,new ArrayList<String>(Arrays.asList("has_impressions_tbl", "has_clicks_tbl","is_profile_query")), 
//    							  new ArrayList<String>(Arrays.asList("0","0","1")));
//    		h = new Hierarchia("mushroom","type");
//    		compute_visualization(placeholderNode, new ArrayList<String>(Arrays.asList("cap_shape","cap_surface","cap_color")), 
//    							new ArrayList<String>(Arrays.asList("f","s","g")));
//    		h = new Hierarchia("titanic","survived");
//    		compute_visualization(placeholderNode, new ArrayList<String>(Arrays.asList("pc_class")), 
//    							new ArrayList<String>(Arrays.asList("3")));
	   Euclidean ed = new Euclidean();
 	   //Hierarchia h = new Hierarchia("mushroom","cap_color");
 	   Hierarchia h = new Hierarchia("titanic","survived");
 	   //Lattice lattice = Hierarchia.generateFullyMaterializedLattice(ed,0.001,0); // Fully materialized lattice
       Lattice lattice = Hierarchia.generateFullyMaterializedLattice(ed,0.001,0.8);
       //Pick all nodes to put in maxSubgraph
       for (int i=0;i<lattice.nodeList.size();i++) {
    	   		lattice.maxSubgraph.add(i);
       }
       VizOutput vo = new VizOutput(lattice, lattice.maxSubgraph, h, "COUNT");
       String nodeDic = vo.generateNodeDic();
       VizOutput.dumpString2File("test.json", nodeDic);
//       System.out.println("# of nodes before merging:"+lattice.nodeList.size());
//       mergeNodes(lattice);
//       System.out.println("# of nodes after merging:"+lattice.nodeList.size());
       // For now, the traversal strategy doesn't work with merging yet, because it is not entirely clear how 
       // we should pick merged nodes. Right now merged nodes has key as the original nodes with an additional 
       // attribute merged_nodes_keys containing a list of keys for the merged nodes, traversal needs to account for this.
       
//       Traversal tr = new FrontierGreedyPicking(lattice,new Euclidean());
//       tr.pickVisualizations(20);
    }
}