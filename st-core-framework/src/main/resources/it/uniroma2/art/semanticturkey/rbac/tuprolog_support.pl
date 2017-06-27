/*******************************
 ** tu-prolog implementation **
 *******************************/

char_subset(CRUDVRequest, CRUDV).
	atom_chars(CRUDVRequest,CRUDVRequestList),
	atom_chars(CRUDV,CRUDVList),
	subset(CRUDVRequestList, CRUDVList).



subset([],_).

subset([H|R],L) :-
	member(H,L),
	subset(R,L).


split_string(LANG,_,_,LANGList) :-
  atom_chars(LANG,LANG_CHARSLIST),
	split_string(LANG_CHARSLIST,[],LANGList).

split_string([],ACC,[Atom]) :- atom_chars(Atom,ACC).

split_string([","|R],ACC,[Atom|LANGList]) :- 
	atom_chars(Atom,ACC),	
	!, 
	split_string(R,[],LANGList).
	
split_string([H|R],ACC,LANGList) :- 
  % NewACC = [H|ACC],
  append(ACC,[H],NewACC), % not very efficient to use append, use an accumulator
	split_string(R,NewACC,LANGList).
