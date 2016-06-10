package helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

public class FilterableList<E> extends ArrayList<E>{
	private static final long serialVersionUID = -5568281161074707171L;


	public interface FilterFunc<RETURN_TYPE, ARG_TYPE> extends Callable<RETURN_TYPE> {
		public RETURN_TYPE call(ARG_TYPE arg);
		public RETURN_TYPE call();
	}

	
	public FilterableList(int size) {
		super(size);
	}


	public FilterableList() {
		super();
	}
	


	public FilterableList(Collection<? extends E> other_list) {
		super(other_list);
	}

	public FilterableList<E> notNullObjects(boolean inplace){
		return this.filter(new FilterFunc<Boolean, E>() {
			@Override
			public Boolean call(E arg) { return arg != null; }
			@Override
			public Boolean call() { return true; }
		}, inplace);
	}
	public FilterableList<E> notNullObjects(){
		return notNullObjects(false);
	}


	public FilterableList<E> filter(FilterFunc<Boolean, E> filter_func) {
		return filter(filter_func, false);
	}
	
	public FilterableList<E> filter(FilterFunc<Boolean, E> filter_func, boolean inplace) {
		FilterableList<E> result = new FilterableList<E>(this.size());
		for(E el: this)
			if(filter_func.call(el))
				result.add(el);

		if(inplace){
			this.clear();
			this.addAll(result);
		}
		return result;
	}


	public FilterableList<E> slice(int i, int j) {
		if( i >= j) return new FilterableList<E>(); 
		return new FilterableList<E>(this.subList(i, j));
	}

	public FilterableList<E> reversed() {
		FilterableList<E> res = new FilterableList<E>(this);
		Collections.reverse(res);
		return res;
	}
	
	public E previous(E el){
		if (el == null) return null;
		int idx = this.indexOf(el);
		return idx - 1 <= 0 ? null : this.get(idx - 1);
	}
	
	public E next(E el){
		if (el == null) return null;
		int idx = this.indexOf(el);
		return idx + 1 >= this.size() ? null : this.get(idx + 1);
	}


	public void setAll(int start, int end, E el) {
		for(int i= start; i < end; i++)
			this.set(i, el);
		
	}

}
