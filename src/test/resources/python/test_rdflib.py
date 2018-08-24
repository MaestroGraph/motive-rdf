import rdflib, os, gzip, time

"""
Some test runs with RDF lib to get values for the unit tests (and speed 
comparisons).



"""

DIR = os.path.dirname(os.path.realpath(__file__))

fn = DIR + os.sep + '../../../main/resources/data/swdf-2012-11-28.nt.gz'

g = rdflib.graph.Graph()

with gzip.open(fn,'r') as file:        
    g.parse(file, format='nt')
    print('loaded.')

    query = """SELECT DISTINCT ?ppr ?year
    WHERE {
       ?ppr <http://swrc.ontoware.org/ontology#year> ?year .
       ?ppr <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://swrc.ontoware.org/ontology#InProceedings> .
    }"""
    
    start_time = time.time()
    results = g.query(query)
    print('query 1 num results:', len(results)) # 3307

    print('query 1 time:       ', time.time() - start_time)
    


    query = """SELECT DISTINCT ?ppr ?year ?tag
    WHERE {
       ?ppr <http://swrc.ontoware.org/ontology#year> ?year .
       ?ppr ?tag <http://swrc.ontoware.org/ontology#InProceedings> .
    }"""
    
    start_time = time.time()
    results = g.query(query)
    print('query 2 num results:', len(results)) # 3307

    print('query 2 time:       ', time.time() - start_time) # 1.0s
    
    
    
    query = """SELECT DISTINCT ?ppr ?year ?tag
    WHERE {
       ?ppr ?tag ?year .
       ?ppr <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://swrc.ontoware.org/ontology#InProceedings> .
    }"""
    
    start_time = time.time()
    results = g.query(query)
    print('query 3 num results:', len(results)) # 81855

    print('query 3 time:       ', time.time() - start_time) # 17.26
    
    
    
    
    