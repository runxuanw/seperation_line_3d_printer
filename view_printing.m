file=fopen('path_dot_output.txt','r');
height=''
while feof(file)==0
    line=fgetl(file)
    if strncmp(line,'Layer:',6)==1 || strcmp(line, 'seperation layer:')==1
    % new layer
        temp_height=fgetl(file)
        
        if strcmp(height,temp_height)==0
            height=temp_height
            figure
            hold off
        end
        
        dot_num=str2num(fgetl(file))
        M=zeros(2,dot_num)
        
        for i=1:dot_num
            line=str2num(fgetl(file))
            M(i*2-1)=line(1)
            M(i*2)=line(2)
        end
        for i=1:dot_num-1
            x=[M(i*2-1),M(i*2+1)]
            y=[M(i*2),M(i*2+2)]
            axis([-1,1,-1,1])
            
            plot(x,y)
            hold on
            %pause(0.0001)
        end
    end
end
fclose(file);
