# This file contains code that constructs the visualization node and lattices
from database import *
from vizObj import *
from node import *
from lattice import *

def generateVizObj(tablename,x_attr,y_attr, agg_func, filters):
	v = vizObj()
	# 	summarization=# SELECT survived, COUNT(id)  FROM titanic WHERE sex='male' AND age<20 GROUP BY survived;
	#  survived | count
	# ----------+-------
	#         0 |    46
	#         1 |    27
	# (2 rows)
	(xVals, yVals) = query_vizData(tablename,x_attr,y_attr, agg_func, filters)
	v.setX(xVals)
	v.setData(yVals) 
	v.setY(agg_func+'('+y_attr+')') 
	v.setFilters(filters)
	v.setAgg(agg_func)
	return v  


def generateLattice(filters):
	if filters is None:
		return None


	# Automatically parse a list of filter as strings and generate lattice of x numbers of nodes given a list of filters 
	# code should automatically infer who is parent and set children based on what's in the filter
	# e.g. since the filter "sex='male'" is contained in the filter "sex='male' AND age<20", and "sex='male'" only has 1 condition
	# the node with the filter "sex='male'" should be the parent
	# assume ordering doesn't matter, users can give you any list of filter values
	
	# 'root' should be special keyword that fires SQL query without the WHERE condition 
	
	# Input: filters= ["root", "sex='male'", "sex='female'", "AGE>20","AGE<20",\
	#  "sex='male' AND AGE>20","sex='female' AND AND AGE>20","sex='male' AND AGE<20","sex='female' AND AND AGE<20"]
	# Output : Lattice containing 9 vizObjs each of them has one of the filter from the filters list
	# with the according parent child relationship set. 
	'''
	G = Lattice()
	root = vizNode()


	nodeList = []

	for each in filters:
	 	if(str(each) == "root"):
	 		    root = vizNode(filters=["All"])
	 		    nodeList.append(root)


	for each in filters:
	 	if(str(each) != "root"):
	 				nodeList.append(vizNode(parents=[root], filters=[str(each)]))



	for idx, val in enumerate(filters):

	 		cuarr = val.split("=")
		if(val != "root"):
	 			current.set_parents(["root"])
		else
	 			continue
	 	for i in range(idx, len(filters)):
	 			cp = filters[i]
	 			cparr = cp.split("=")
	 			if cparr[0] != cuarr[0]: #different filter title
	 				combined = val + " AND " + cp

	 				combinedNode = vizNode(parents=[White, M], filters=["Race = White", "Gender = M"])
'''
pass