% This file is the implementation of rbac_tbox.pl for tuProlog

% It is the version used by the RBAC checker in Semantic Turkey

/*

auth � il primo step, che invoca chk_capability verificando poi il CRUDV. CRUDV � CRUD pi� "Validate". 
Note that validate should be together with other powers (a user can have the power to validate creation actions, validate update actions and so on)
chk_capability svolge diverse espansioni a livello TBOX prima di guardare i fatti, che controlla su capability (i fatti)

*/

% CRUDV is always a list 
auth(TOPIC, CRUDVRequest) :-
%	write('enter-auth | '),
	chk_capability(TOPIC, CRUDV),
	resolveCRUDV(CRUDVRequest, CRUDV).


chk_capability(TOPIC, CRUDV) :-
%	write('check plain capability correspondence | '),
	capability(TOPIC, CRUDV).


/*********************************
 ** rules and shortcuts for rdf **
 *********************************/

chk_capability(rdf(_), CRUDV) :-	
%	write('exp: rdf into rdf(_) | '),
	chk_capability(rdf, CRUDV).	

chk_capability(rdf(_,_), CRUDV) :-	
%	write('exp: rdf into rdf(_,_)  | '),
	chk_capability(rdf, CRUDV).
	
chk_capability(rdf(Subject), CRUDV) :- 
%	write('chk | '),
	capability(rdf(AvailableSubject), CRUDV),
	covered(Subject, AvailableSubject).	
	
chk_capability(rdf(Subject,Scope), CRUDV) :-
%	write('chk-with-scope | '),
	capability(rdf(AvailableSubject,Scope), CRUDV),
	covered(Subject, AvailableSubject).

chk_capability(rdf(Subject,lexicalization(LANG)), CRUDV) :-
%	write('chk-with-lexscope | '),
	capability(rdf(AvailableSubject,lexicalization(LANGCOVERAGE)), CRUDV),
	covered(Subject, AvailableSubject),
	resolveLANG(LANG, LANGCOVERAGE).

/*****************************
** shortcuts for rdf(skos). **
*****************************/

chk_capability(rdf(SKOSELEMENT), CRUDV) :-
	capability(rdf(skos), CRUDV),
	vocabulary(SKOSELEMENT, skos).
	
chk_capability(rdf(SKOSELEMENT,_), CRUDV) :-
	capability(rdf(skos), CRUDV),
	vocabulary(SKOSELEMENT, skos).
	
	
/*****************************
** shortcuts for rdf(ontolex). **
*****************************/

chk_capability(rdf(ONTOLEXELEMENT), CRUDV) :-
	capability(rdf(ontolex), CRUDV),
	vocabulary(ONTOLEXELEMENT, ontolex).
	
chk_capability(rdf(ONTOLEXELEMENT,_), CRUDV) :-
	capability(rdf(ontolex), CRUDV),
	vocabulary(ONTOLEXELEMENT, ontolex).	

	
% this tells that a simple rdf(lexicalization(LANG)) implies to be able to lexicalize (in LANG) for every resource	
% and to create/edit skosxl:Labels(LANG) as well.

chk_capability(rdf(_,lexicalization(LANG)), CRUDV) :-
%	write('lexicalization expansion for simple labels for any resource| '),
	capability(rdf(lexicalization(LANGCOVERAGE)), CRUDV),
	resolveLANG(LANG, LANGCOVERAGE).

chk_capability(rdf(xLabel(LANG)), CRUDV) :-
%	write('lexicalization expansion for cruding SKOSXL labels | '),
	capability(rdf(lexicalization(LANGCOVERAGE)), CRUDV),
	resolveLANG(LANG, LANGCOVERAGE).

chk_capability(rdf(xLabel(LANG),_), CRUDV) :-
%	write('lexicalization expansion for editing SKOSXL labels  | '),
	capability(rdf(lexicalization(LANGCOVERAGE)), CRUDV),
	resolveLANG(LANG, LANGCOVERAGE).

% expansions for all languages for general lexicalization
chk_capability(rdf(_,lexicalization(_)), CRUDV) :-
%	write('lexicalization expansion for simple labels for any resource| '),
	capability(rdf(lexicalization), CRUDV).

chk_capability(rdf(xLabel(_)), CRUDV) :-
%	write('lexicalization expansion for cruding SKOSXL labels | '),
	capability(rdf(lexicalization), CRUDV).

chk_capability(rdf(xLabel(_),_), CRUDV) :-
%	write('lexicalization expansion for editing SKOSXL labels  | '),
	capability(rdf(lexicalization), CRUDV).
	
	
chk_capability(rdf(_,lexicalization), CRUDV) :-
%	write('lexicalization expansion for simple labels for any resource| '),
	capability(rdf(lexicalization), CRUDV).

chk_capability(rdf(xLabel), CRUDV) :-
%	write('lexicalization expansion for cruding SKOSXL labels | '),
	capability(rdf(lexicalization), CRUDV).

chk_capability(rdf(xLabel,_), CRUDV) :-
%	write('no language lexicalization expansion for editing SKOSXL labels  | '),
	capability(rdf(lexicalization), CRUDV).	
	

chk_capability(rdf(ontolexForm), CRUDV) :-
%	write('lexicalization expansion for cruding SKOSXL labels | '),
	capability(rdf(lexicalization), CRUDV).

chk_capability(rdf(ontolexForm,_), CRUDV) :-
%	write('no language lexicalization expansion for editing SKOSXL labels  | '),
	capability(rdf(lexicalization), CRUDV).	
	
chk_capability(rdf(ontolexLexicalEntry), CRUDV) :-
%	write('lexicalization expansion for cruding SKOSXL labels | '),
	capability(rdf(lexicalization), CRUDV).

chk_capability(rdf(ontolexLexicalEntry,_), CRUDV) :-
%	write('no language lexicalization expansion for editing SKOSXL labels  | '),
	capability(rdf(lexicalization), CRUDV).		

chk_capability(rdf(limeLexicon), CRUDV) :-
%	write('lexicalization expansion for cruding SKOSXL labels | '),
	capability(rdf(lexicalization), CRUDV).

chk_capability(rdf(limeLexicon,_), CRUDV) :-
%	write('no language lexicalization expansion for editing SKOSXL labels  | '),
	capability(rdf(lexicalization), CRUDV).			
	
chk_capability(rdf(_,notes), CRUDV) :-
% write('notes expansion'),
capability(rdf(notes), CRUDV).





/*********************************
 ** rules and shortcuts for rbac **
 *********************************/

chk_capability(rbac(_), CRUDV) :-	
%	write('exp: rbac into rbac(_) | '),
	chk_capability(rbac, CRUDV).	

chk_capability(rbac(_,_), CRUDV) :-	
%	write('exp: rbac into rbac(_,_)  | '),
	chk_capability(rbac, CRUDV).


/*********************
 ** CORE PREDICATES **
 *********************/


resolveCRUDV(CRUDVRequest, CRUDV) :-
	char_subset(CRUDVRequest, CRUDV).
	
resolveLANG(LANG, LANGCOVERAGE) :-
  % format('resolve lang: [~w] vs [~w]', [LANG, LANGCOVERAGE]),
  split_string(LANG,",","",LANGList),
  split_string(LANGCOVERAGE,",","",LANGCOVERAGEList),
	subset(LANGList, LANGCOVERAGEList).

% undetermined, cls, individual, property, objectProperty, datatypeProperty, annotationProperty, ontologyProperty, ontology, dataRange, concept, conceptScheme, xLabel, skosCollection, skosOrderedCollection;

covered(Subj,resource) :- role(Subj).
covered(objectProperty, property).
covered(datatypeProperty, property).
covered(annotationProperty, property).
covered(ontologyProperty, property).
covered(skosOrderedCollection, skosCollection).
covered(Role, Role).	


role(cls).
role(individual).
role(property).
role(objectProperty).
role(datatypeProperty).
role(annotationProperty).
role(ontologyProperty).
role(ontology).
role(dataRange).
role(concept).
role(conceptScheme).
role(xLabel).
role(xLabel(_)).
role(skosCollection).
role(ontolexForm).
role(ontolexLexicalEntry).
role(limeLexicon).
	
vocabulary(concept, skos).
vocabulary(conceptScheme, skos).
vocabulary(skosCollection, skos).

vocabulary(ontolexForm, ontolex).
vocabulary(ontolexLexicalEntry, ontolex).
vocabulary(limeLexicon, ontolex).


	
/****************************
 ** INTERACTION PREDICATES **
 ****************************/
	
getCapabilities(FACTLIST) :- findall(capability(A,CRUD),capability(A,CRUD),FACTLIST).	
	
/*
LOST EXPANSIONS

capability(_, CRUDV) :-	
	capability(*, CRUDV).

capability(rdf(_), CRUDV) :-		
	capability(rdf(*), CRUDV) :-		


*/
