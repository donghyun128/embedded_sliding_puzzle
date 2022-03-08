#pragma OPENCL EXTENSION cl_khr_fp64 : enable

__kernel void kernel_grid(__global unsigned int *src,__global unsigned int *dst, const int width, const int height,const int size)
{
	int row = get_global_id(0) / width; 
	int col = get_global_id(0) % width;
	int pix = row*width + col;
	
	int bound_x = width / size;
	int bound_y = height / size;
	
		if ((row % bound_y == 0) || (col % bound_x == 0)||(row == height-1) ||(col == width-1))
		{
	 	dst[pix] = 0;
		}
		else
		{
	 	dst[pix] = src[pix];
		}
	
	
}
