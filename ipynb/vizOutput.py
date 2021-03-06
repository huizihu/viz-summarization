import json
import string
import math
import matplotlib.pyplot as plt
import numpy as np

millnames = ['','E3',' E6',' E9','E12']

def millify(n):
    n = float(n)
    millidx = max(0,min(len(millnames)-1,
                        int(math.floor(0 if n == 0 else math.log10(abs(n))/3))))
    if millidx<2:
        return int(n)
    else:
        return '{:.0f}{}'.format(n / 10**(3 * millidx), millnames[millidx])
def get_cmap(n, name='hsv'):
    '''Returns a function that maps each index in 0, 1, ..., n-1 to a distinct 
    RGB color; the keyword argument name must be a standard mpl colormap name.'''
    return plt.cm.get_cmap(name, n)
def autolabel(rects, ax):
    # Get y-axis height to calculate label position from.
    (y_bottom, y_top) = ax.get_ylim()
    y_height = y_top - y_bottom

    for rect in rects:
        height = rect.get_height()
        label_position = height + (y_height * 0.01)

        ax.text(rect.get_x() + rect.get_width()/2., label_position-(height/2),
                '%.1f' % height,
                ha='center', va='bottom',fontsize=11)
def ax_bar_chart(ax,yVals,xAttrs,xtitle="",ytitle="",title="",top_right_text="", N=1,width=0.1):
    ind = np.arange(N)  # the x locations for the groups
    
    cmap = get_cmap(len(yVals)+1)
    rects = []
    for i in range(len(yVals)):
        rect = ax.bar(ind + i*width, yVals[i], width, color=cmap(i),  ecolor= "black")
        rects.append(rect)

    # add some text for labels, title and axes ticks
    #ax.set_ylabel(ytitle,fontsize=14)
    title = ax.set_title(title,fontsize=12)
    
    # Left vertical title
    #title.set_position((1.1,0.9))
    #title.set_rotation(270)

    xmin = -0.05
    xmax = 0.25+0.1*(len(yVals)-2)
    xtickpos = [np.abs(xmin-xmax)/(len(yVals)+2)*(i+0.7) for i in range(len(yVals))]
    ax.set_xticks(xtickpos)
    ax.set_xticklabels(xAttrs,fontsize=12)
    #ax.set_xlabel(xtitle,fontsize=12)

    #ax.legend((rects1[0], rects2[0]), xAttrs)
    ax.annotate(top_right_text, xy=(0.75, 0.85), xycoords='axes fraction')
    ax.set_xlim(xmin,xmax)
    ax.set_ylim((0,100))

    for rect in rects:
        autolabel(rect, ax)
def generateDashboard(nodeDicStr,xlabel,ylabel,numBars,title,fname=""):    
    nodeDic=json.loads(nodeDicStr)
    fig, axs = plt.subplots(nrows=4,ncols=5,figsize=(10,8),sharex=True,sharey=True)
    axs = axs.ravel()
    xAttrs = []
    yValsLsts = []
    filterLsts = []
    popSizeLsts = [] 
    for it,node in enumerate(nodeDic.values()):
        yVals =[]
        filterName=""
        for i,bar in enumerate(node): 
            if i<numBars:
                if it==0:
                    xAttrs.append(bar["xAxis"])
                yVals.append(bar["yAxis"])
            if i==numBars:
                filterName=bar['filter']
                if filterName=="#":
                    filterName="Root"
                else:
                    filterName=string.join(filterName.split("#")[1:-1],";\n").replace("$","=")
                popSize = bar["populationSize"]
        yValsLsts.append(yVals)
        filterLsts.append(filterName)
        popSizeLsts.append(popSize)
    level = [f.count(";")+1 for f in filterLsts]
    sortedIdx = list(np.argsort(level))
    try:
        rootIdx = filterLsts.index("Root")
        sortedIdx.remove(rootIdx)
        sortedIdx = np.array([rootIdx]+sortedIdx)
    except(ValueError):
        print "no root? "
        sortedIdx = np.array(sortedIdx)
    filterLsts= np.array(filterLsts)[sortedIdx]
    yValsLsts =  np.array(yValsLsts)[sortedIdx]
    popSizeLsts = np.array(popSizeLsts)[sortedIdx]
    for ax,yVals,filterName,popSize in zip(axs,yValsLsts,filterLsts,popSizeLsts):
        ax_bar_chart(ax,yVals,xAttrs,"survived",'COUNT(id)',filterName,millify(popSize))
    fig.text(0.5, -0.01, xlabel, ha='center',fontsize=14)
    fig.text(-0.01, 0.5, ylabel, va='center', rotation='vertical',fontsize=14)
    fig.text(0.5, 1, title, ha='center',fontsize=14)
    plt.tight_layout(rect=[0.01, 0.01, 1, 1])
    if fname!="": plt.savefig("dashboards/"+fname,bbox_inches='tight')