

class Move
{
    private int rowDepart, colDepart;
    private int rowArrive, colArrive;

    public Move(){
        rowDepart = -1;
        colDepart = -1;
        rowArrive = -1;
        colArrive = -1;
    }

    public Move(int rd, int cd, int ra, int ca){
        rowDepart = rd;
        colDepart = cd;
        rowArrive = ra;
        colArrive = ca;
    }

    public int getRowDepart(){
        return rowDepart;
    }

    public int getColDepart(){
        return colDepart;
    }

    public int getRowArrive(){
        return rowArrive;
    }

    public int getColArrive(){
        return colArrive;
    }

    public void setRowDepart(int r){
        rowDepart = r;
    }

    public void setColDepart(int c){
        colDepart = c;
    }

    public void setRowArrive(int r){
        rowArrive = r;
    }

    public void setColArrive(int c){
        colArrive = c;
    }
}
