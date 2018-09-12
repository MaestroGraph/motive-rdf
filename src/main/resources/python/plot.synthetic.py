# -*- coding: utf-8 -*-

import matplotlib as mpl
from _socket import NI_DGRAM
mpl.use('Agg')
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats
import networkx as nwx
from networkx.drawing import nx_agraph
import pygraphviz
import glob
import builtins
from matplotlib.pyplot import margins
import os.path
import json
from io import StringIO, BytesIO
from PIL import Image

# How many motifs to plot
NUMPLOT = 10

RED = 'darkred'
G1 = 'grey'
G2 = 'teal'
G3 = 'black'

mpl.style.use('classic')

font = {'family' : 'normal',
        'weight' : 'normal',
        'size'   : 15}

mpl.rc('font', **font)

margin = 0.05
extra = 0.05

row1height = 0.6
row2height = 0.2
row3height = 0.2

# To be run in the workspace dir of the UCompareBeta module
barwidth = 0.9
pluswidth = 0.45

# Load experiment metadata
# with open('metadata.json') as mdfile:
#     metadata = json.load(mdfile)

# sub_index = metadata["subindex"]
# nums_instances = metadata["nums instances"]
# motif_size = metadata["motif size"]
# directed = False

ni = 3 # len(nums_instances)

# Load the frequencies and factors
raw = n.genfromtxt('scores.csv', delimiter=',')
(nummotifs, width) = raw.shape

# raw = n.nan_to_num(raw)
# raw[raw == -2147483648] = 0
assert nummotifs >= NUMPLOT

frequencies = raw[:, (1, 3, 5)]
factors = raw[:, (0, 2, 4)]

fig = p.figure(figsize=(8,4))

### 1) Plot the factors
ax1 = fig.add_axes([0.0 + margin + extra, row3height + row2height + margin, 1.0 - 2.0 * margin- extra, row1height - 2.0 * margin]); 

ind = n.arange(NUMPLOT)

bw = barwidth/ni
for i in range(ni):
    color = G1
    label = u'$k = 0$'
    if i == 1:
        color = G2
        label = u'$k = 75$'
    if i == 2:
        color = G3
        label = u'$k = 150$'
    
    print(i, factors[:NUMPLOT, i])
    
    bars = ax1.bar(ind - barwidth/2.0 + i * bw, factors[:NUMPLOT, i], bw, color=color, zorder=1, linewidth=0)
    bars.set_label(label)

# Error bars    
# for i in range(ni):
#     for s in range(nummotifs):
#         
#         min = n.min(factors[s,i*runs:(i+1)*runs])
#         max = n.max(factors[s,i*runs:(i+1)*runs])
#         ax1.vlines((ind[s] - barwidth/2.0 + (i+0.5) * bw),min, max, colors=RED, linewidths=2, zorder=3)
   
ax1.set_xlim([0 - pluswidth, NUMPLOT - 1 + pluswidth])

ax1.hlines(0, - pluswidth, NUMPLOT - 1 + pluswidth)

yloc = p.MaxNLocator(7)
ax1.get_yaxis().set_major_locator(yloc)

ax1.get_yaxis().set_tick_params(which='both', direction='out')

ax1.spines["right"].set_visible(False)
ax1.spines["top"].set_visible(False)
ax1.spines["bottom"].set_visible(False)
ax1.spines["left"].set_visible(False)

ax1.get_xaxis().set_tick_params(which='both', top='off', bottom='off', labelbottom='off')
ax1.get_yaxis().set_tick_params(which='both', left='off', right='off')

# top = n.max(factor)
# if n.min(factor) < - top and top > 0:
#   ax1.set_ylim(bottom=-top)
   
# negative grid (white lines over the bars)   
ticks = ax1.get_yaxis().get_majorticklocs()   
ticks = n.delete(ticks, n.where(n.logical_and(ticks < 0.00001, ticks > -0.00001)))
ax1.hlines(ticks, - pluswidth, NUMPLOT - 1 + pluswidth, color='w', zorder=2)

ax1.legend()
ax1.set_ylabel('log-factor (bits)')

### 2) Plot the motifs

bottom = margin
height = row2height - margin

side = pluswidth - 0.5
width = (1.0 - 2.0 * margin - extra) / (NUMPLOT + 2.0 * side)

with open('motifs.csv', 'r') as file:    
    for i, dotstring in enumerate(file):
        if i >= NUMPLOT:
            break;
        
        print('.')
        
        axsmall = fig.add_axes([margin + extra + side*width + width * i, bottom, width, height])
        axsmall.axis('off')
        
        graph = pygraphviz.AGraph(dotstring)
        
        drawn = BytesIO()
        graph.draw(drawn, format='png', prog='neato', args=' -Gdpi=1200')
        
        im = Image.open(drawn)
        axsmall.imshow(im, interpolation='none')
    
### 3)  Frequency graph

ax3 = fig.add_axes([0.0 + margin + extra, row2height + margin, 1.0 - 2.0 * margin - extra, row3height - margin]) 

# ax3.bar(ind - barwidth/2.0, freq, barwidth, color='k')
for i in range(ni):
    color = G1
    if i == 1:
        color = G2
    if i == 2:
        color = G3
    
    # the means as bars
    ax3.bar(ind - barwidth/2.0 + i * bw, frequencies[:NUMPLOT, i], bw, color=color, zorder=1, linewidth=0)

ax3.get_yaxis().set_tick_params(which='both', direction='out')

ax3.set_xlim([0 - pluswidth, NUMPLOT - 1 + pluswidth])
# ax3.set_xlim(lower=0)

# reduce the number of ticks
yloc = p.MaxNLocator(4)
ax3.yaxis.set_major_locator(yloc)

ax3.spines["right"].set_visible(False)
ax3.spines["top"].set_visible(False)
ax3.spines["left"].set_visible(False)

ax3.get_xaxis().tick_bottom()
ax3.get_xaxis().set_tick_params(which='both', top='off', bottom='off', right='off', labelbottom='off')
ax3.get_yaxis().set_tick_params(which='both', left='off', right='off')

ax3.set_ylim([0, ax3.get_ylim()[1]])

ticks = ax3.get_yaxis().get_majorticklocs()   
ticks = n.delete(ticks, n.where(n.logical_and(ticks < 0.00001, ticks > -0.00001)))
ax3.hlines(ticks, - pluswidth, NUMPLOT - 1 + pluswidth, color='w', zorder=2)

ax3.set_ylabel('freq.')

p.savefig('synthetic-plot.png', dpi=1200)
p.savefig('synthetic-plot.pdf', dpi=1200)
