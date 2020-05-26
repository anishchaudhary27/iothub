import tkinter

window = tkinter.Tk()
window.title('test window')
# widgets added here

button = tkinter.Button(window,text='stop',width=25,command=window.destroy)
button.pack()
# till  here
window.mainloop()