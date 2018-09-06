# -*- coding: utf-8 -*-

import matplotlib as mpl
mpl.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
import pylab
from matplotlib.pyplot import margins
import os.path
import json, sys
from io import StringIO, BytesIO
from PIL import Image

RED = 'darkred'
G1 = 'lightgrey'
G2 = 'silver'
G3 = 'darkgrey'

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
raw = np.genfromtxt('synthrep.csv', delimiter=',')
(numexperiments, width) = raw.shape

sizes = [7611, 8285, 23644] # TODO: FIX after final run
linksizes = [242256, 29226, 74567]
relsizes  = [170, 47, 24]

arrays = {}
for size in sizes:
    arrays[size] = raw[np.where(raw[:, 0] == size)]
    
fig, axes = plt.subplots(3, 1, sharex=True, squeeze=True, figsize=(8,9))

### 1) Plot the factors

split = 20

for i, (ax, size) in enumerate(zip(axes, sizes)):  
    
    ax.set_title('n={}, m={}, r={}'.format(size, linksizes[i], relsizes[i]) ) 
    data = arrays[size]
    
    # hnoise = np.random.randn(*(data[:, 3].shape)) * 0.05 # horizontal jitter
    paths = ax.scatter(data[:, 3], data[:,4] - data[:,5], c=data[:, 7], s=(data[:,7]) ** 0.65 + 5, cmap='viridis', linewidth=0, alpha=0.4) # RdYlBu

    # plt.colorbar()
    
    yloc = plt.MaxNLocator(5)
    ax.get_yaxis().set_major_locator(yloc)

    ax.get_yaxis().set_tick_params(which='both', direction='out')

    ax.spines["right"].set_visible(False)
    ax.spines["top"].set_visible(False)
    
    ax.spines["bottom"].set_visible(True)
    ax.spines['bottom'].set_position('zero')
    
    ax.spines["left"].set_visible(True)
    
    ax.get_xaxis().set_tick_params(which='both', top='off')
    ax.get_yaxis().set_tick_params(which='both', right='off')
    ax.set_ylabel('log-factor')
    
    ax.set_xlim(0, max(data[:, 3]))


fig.subplots_adjust(right=0.90)
cbar_ax = fig.add_axes([0.92, 0.55, 0.02, 0.4])
cbar = fig.colorbar(paths, cax=cbar_ax)

#cbar.solids.set_rasterized(True)
cbar.solids.set_edgecolor("face")
cbar.set_alpha(1)
cbar.draw_all()

axes[-1].set_xlabel('number of motifs added')

# plt.colorbar(paths, cax=axes[0])
    
plt.savefig('synthrep-plot.png')
plt.savefig('synthrep-plot.pdf')

sys.exit()
