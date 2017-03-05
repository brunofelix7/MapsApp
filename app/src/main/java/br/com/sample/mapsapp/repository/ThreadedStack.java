package br.com.sample.mapsapp.repository;

import android.util.Log;

public class ThreadedStack {

    private Node top;
    private int size = 0;

    /**
     * Verifica se está vazia
     */
    public boolean isEmpty(){
        return top == null;
    }

    /**
     * Retorna o tamanho da pilha
     */
    public int stackSize(){
        return size;
    }

    /**
     * Insere um novo Nó na pilha
     */
    public boolean stackAdd(String data){
        Node newNode = new Node();
        newNode.setData(data);
        if(isEmpty()){
            newNode.setNext(top);
        }
        top = newNode;
        size++;
        return true;
    }

    /**
     * Retorna o topo da pilha
     */
    public String getTop(){
        if(isEmpty()){
            return "Stack empty!";
        }
        return top.getData();
    }

    /**
     * Remove um elemento (Topo)
     */
    public String stackRemove(){
        if(isEmpty()){
            return "Stack empty";
        }
        String data = top.getData();
        top = top.getNext();
        size--;
        return data;
    }

    /**
     * Imprime todos os elementos da pilha
     */
    public void stackPrint(){
        Node helper = top;
        while(helper != null){
            Log.d("StackPrint", helper.getData());
            helper = helper.getNext();
        }
    }
}
