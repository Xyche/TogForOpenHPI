package helper;

import java.util.ArrayList;
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


	public FilterableList(ArrayList<E> other_list) {
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

}
